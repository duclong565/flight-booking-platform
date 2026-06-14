package com.deanflights.flight.event;

import com.deanflights.flight.Flight;

/**
 * A domain event: "a flight was created". This is the message we publish to Kafka.
 *
 * <p>It is a small, immutable snapshot — NOT the JPA entity. We deliberately send only the
 * facts other parts of the system care about, so consumers don't depend on our DB shape.
 * (Same idea as a DTO, but for messaging instead of HTTP.)
 *
 * <p>Records serialize cleanly to/from JSON via Jackson, which is what Spring Kafka's
 * JsonSerializer/JsonDeserializer use.
 */
public record FlightCreatedEvent(
        Long flightId,
        String flightNumber,
        String origin,
        String destination,
        int totalSeats
) {
    /** The Kafka topic this event is published to. Referenced by the publisher and listener. */
    public static final String TOPIC = "flights.created";

    /** Build the event from a freshly-saved Flight entity. */
    public static FlightCreatedEvent from(Flight flight) {
        return new FlightCreatedEvent(
                flight.getId(),
                flight.getFlightNumber(),
                flight.getOrigin(),
                flight.getDestination(),
                flight.getTotalSeats());
    }
}
