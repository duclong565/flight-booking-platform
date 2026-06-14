package com.deanflights.flight;

import com.deanflights.common.NotFoundException;
import com.deanflights.flight.dto.CreateFlightRequest;
import com.deanflights.flight.event.FlightCreatedEvent;
import com.deanflights.flight.event.FlightEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

/**
 * Business logic for flights. Kept deliberately thin in this slice: map the request
 * to an entity, set the starting seat count, and delegate persistence to the repository.
 */
@Service
public class FlightService {

    private final FlightRepository flightRepository;
    private final FlightEventPublisher eventPublisher;

    // Constructor injection — Spring passes these beans in automatically.
    public FlightService(FlightRepository flightRepository, FlightEventPublisher eventPublisher) {
        this.flightRepository = flightRepository;
        this.eventPublisher = eventPublisher;
    }

    public Flight create(CreateFlightRequest request) {
        Flight flight = new Flight();
        flight.setFlightNumber(request.flightNumber());
        flight.setOrigin(request.origin());
        flight.setDestination(request.destination());
        flight.setDepartureTime(request.departureTime());
        flight.setArrivalTime(request.arrivalTime());
        flight.setTotalSeats(request.totalSeats());
        flight.setAvailableSeats(request.totalSeats()); // a brand-new flight starts full
        flight.setBasePrice(request.basePrice());

        Flight saved = flightRepository.save(flight);

        // Publish the event AFTER the flight is safely persisted.
        // (Note: this isn't transactional with the DB write yet — if publishing fails the
        //  flight is still saved. The "transactional outbox" pattern fixes that; a later lesson.)
        eventPublisher.publishFlightCreated(FlightCreatedEvent.from(saved));

        return saved;
    }

    public Flight getById(Long id) {
        return flightRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Flight not found: " + id));
    }

    public Page<Flight> search(String origin, String destination, LocalDate departureDate, Pageable pageable) {
        Instant from = null;
        Instant to = null;
        if (departureDate != null) {
            // Single-day window [from, to): start of the day to start of the next day, UTC.
            from = departureDate.atStartOfDay().toInstant(ZoneOffset.UTC);
            to = departureDate.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        }
        return flightRepository.search(origin, destination, from, to, pageable);
    }
}
