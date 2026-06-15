# Architecture

The system is described with the [C4 model](https://c4model.com): **Context → Container → Component**. Diagrams are Mermaid (rendered by GitHub).

The service is built as a **modular monolith** today, with clean bounded contexts that are designed to be extractable into separate services as scaling needs justify it. Both the current shape and the target (microservices) shape are shown below.

---

## Level 1 — System Context

Who uses the system and what it depends on.

```mermaid
C4Context
    title System Context — Flight Booking Platform

    Person(traveler, "Traveler", "Searches flights and makes bookings")
    Person(admin, "Operations / Admin", "Manages flight inventory")

    System(fbp, "Flight Booking Platform", "Search flights, reserve seats, book")

    System_Ext(idp, "Identity (OAuth2/OIDC)", "Planned: external IdP (Keycloak)")
    System_Ext(payment, "Payment Provider", "Planned: processes payments")
    System_Ext(notify, "Notification Service", "Planned: email/SMS confirmations")

    Rel(traveler, fbp, "Searches & books flights", "HTTPS / REST")
    Rel(admin, fbp, "Manages flights", "HTTPS / REST")
    Rel(fbp, payment, "Requests payment", "HTTPS")
    Rel(fbp, notify, "Sends confirmations", "async")
```

Authentication is currently handled in-app with self-issued JWTs; an external identity provider is a planned evolution.

---

## Level 2 — Containers (current)

Everything booking-related runs in a single Spring Boot application backed by PostgreSQL, with Kafka for domain events.

```mermaid
C4Container
    title Containers — current (modular monolith)

    Person(traveler, "Traveler")
    Person(admin, "Operations / Admin")

    System_Boundary(fbp, "Flight Booking Platform") {
        Container(app, "booking-service", "Spring Boot 3.5 / Java 21", "REST /api/v1 — flight + booking + auth feature modules")
        ContainerDb(db, "PostgreSQL", "Relational DB", "flights, bookings, users")
        ContainerQueue(kafka, "Apache Kafka", "Event bus", "flights.created, bookings.created")
    }

    Rel(traveler, app, "Search & book (JWT)", "HTTPS/REST")
    Rel(admin, app, "Manage flights (ADMIN)", "HTTPS/REST")
    Rel(app, db, "Reads/writes", "JPA / JDBC")
    Rel(app, kafka, "Publishes & consumes domain events")
```

**Why a monolith first:** clean module boundaries are enforced *in-process* (fast, refactorable, one deployment) so the correct bounded contexts can be discovered cheaply before any of them is promoted to a network-boundary service.

---

## Level 2 — Containers (target)

The intended evolution once independent scaling and fault isolation justify the distributed-systems cost.

```mermaid
C4Container
    title Containers — target (event-driven microservices)

    Person(traveler, "Traveler")

    System_Boundary(fbp, "Flight Booking Platform") {
        Container(gw, "API Gateway", "Spring Cloud Gateway", "Routing, authN, rate limiting")
        Container(booking, "booking-service", "Spring Boot", "Bookings + saga orchestration")
        Container(inventory, "inventory-service", "Spring Boot", "Seat inventory + concurrency")
        Container(search, "search", "Spring Boot + Elasticsearch", "Flight search read model")

        ContainerDb(bdb, "Booking DB", "PostgreSQL", "bookings")
        ContainerDb(idb, "Inventory DB", "PostgreSQL", "flights, seats")
        ContainerDb(redis, "Redis", "Cache", "hot reads, seat holds")
        ContainerQueue(kafka, "Apache Kafka", "Event bus", "domain events + saga")
        ContainerDb(es, "Elasticsearch", "Search index", "flight search")
    }

    Rel(traveler, gw, "HTTPS/REST")
    Rel(gw, booking, "routes", "REST")
    Rel(gw, search, "routes search", "REST")
    Rel(booking, inventory, "reserve/release seats", "gRPC (sync)")
    Rel(booking, kafka, "publishes/consumes", "events / saga")
    Rel(inventory, kafka, "publishes/consumes", "events")
    Rel(search, kafka, "consumes flight events", "build index")
    Rel(booking, bdb, "JPA")
    Rel(inventory, idb, "JPA")
    Rel(booking, redis, "cache")
    Rel(search, es, "index/query")
```

Key target decisions: **gRPC** for the low-latency synchronous booking→inventory hop; **Kafka** for asynchronous domain events and the booking↔payment↔ticketing **saga**; **database-per-service**; **Elasticsearch** as an event-fed read model.

---

## Level 3 — Components (`booking-service`)

Inside the current application: feature-first packages, each a vertical slice `Controller → Service → Repository → DB`.

```mermaid
C4Component
    title Components — booking-service

    Container_Boundary(bs, "booking-service") {
        Component(sec, "Security", "Spring Security 6 + JWT filter", "authN/authZ, RFC 7807 401/403")
        Component(auth, "Auth", "register / login", "issues JWTs, BCrypt")

        Component(fc, "FlightController", "REST /api/v1/flights", "create / search / get")
        Component(fs, "FlightService", "@Service", "flight use cases")
        Component(fr, "FlightRepository", "Spring Data JPA", "flight persistence")

        Component(bc, "BookingController", "REST /api/v1/bookings", "create / get")
        Component(bsvc, "BookingService", "@Service @Transactional", "seat check + decrement, price, publish event")
        Component(br, "BookingRepository", "Spring Data JPA", "booking persistence")

        Component(pub, "Event publishers", "Spring Kafka", "FlightCreated / BookingCreated")
        Component(lst, "Event listeners", "@KafkaListener", "react to domain events")
        Component(geh, "GlobalExceptionHandler", "@RestControllerAdvice", "RFC 7807 ProblemDetail")
    }

    ContainerDb(db, "PostgreSQL", "", "flights, bookings, users")
    ContainerQueue(kafka, "Apache Kafka", "", "domain events")

    Rel(sec, auth, "delegates login")
    Rel(fc, fs, "calls")
    Rel(fs, fr, "uses")
    Rel(bc, bsvc, "calls")
    Rel(bsvc, br, "uses")
    Rel(bsvc, fr, "reads flight + decrements seats")
    Rel(bsvc, pub, "publishes BookingCreated")
    Rel(pub, kafka, "produces")
    Rel(kafka, lst, "delivers")
    Rel(fr, db, "JPA")
    Rel(br, db, "JPA")
```

**The extraction seam:** `BookingService` calling `FlightRepository` to check and decrement seats is the one cross-feature edge. `Booking` references a flight by **ID** (not a JPA association), keeping the aggregates decoupled — so this in-process call becomes the gRPC call to a future `inventory-service` without a rewrite.

## Cross-cutting conventions

- **Errors:** RFC 7807 Problem Details (`application/problem+json`) for all error responses, including security (401/403).
- **API:** versioned under `/api/v1`; resources returned directly (no envelope); pagination via Spring Data `Page`.
- **Persistence:** Spring Data JPA / Hibernate over PostgreSQL.
- **Events:** JSON-serialized domain events on Kafka topics `flights.created` and `bookings.created`.
