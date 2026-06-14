package com.deanflights.booking;

import com.deanflights.booking.dto.CreateBookingRequest;
import com.deanflights.booking.event.BookingCreatedEvent;
import com.deanflights.booking.event.BookingEventPublisher;
import com.deanflights.common.BusinessRuleException;
import com.deanflights.common.NotFoundException;
import com.deanflights.flight.Flight;
import com.deanflights.flight.FlightRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Business logic for bookings. This is the first slice that spans two features: it reads
 * (and decrements) Flight inventory while creating a Booking, so the whole thing runs in one
 * transaction.
 */
@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final FlightRepository flightRepository;
    private final BookingEventPublisher eventPublisher;

    // Constructor injection — Spring passes these beans in automatically.
    public BookingService(BookingRepository bookingRepository,
                          FlightRepository flightRepository,
                          BookingEventPublisher eventPublisher) {
        this.bookingRepository = bookingRepository;
        this.flightRepository = flightRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Booking create(CreateBookingRequest request) {
        // Inventory lives in the Flight feature; we look it up by id.
        Flight flight = flightRepository.findById(request.flightId())
                .orElseThrow(() -> new NotFoundException("Flight not found: " + request.flightId()));

        if (flight.getAvailableSeats() < request.seats()) {
            throw new BusinessRuleException(
                    "Not enough seats: requested %d, available %d".formatted(request.seats(), flight.getAvailableSeats()));
        }

        // Read-modify-write seat decrement. This is a naive, non-concurrency-safe approach kept
        // intentionally simple for now: two bookings racing on the last seats can both pass the
        // check above and oversell. Real seat-concurrency (optimistic locking) is a later lesson,
        // for when inventory becomes its own service.
        flight.setAvailableSeats(flight.getAvailableSeats() - request.seats());
        flightRepository.save(flight);

        BigDecimal totalPrice = flight.getBasePrice().multiply(BigDecimal.valueOf(request.seats()));

        Booking booking = new Booking();
        booking.setBookingReference("BK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        booking.setFlightId(flight.getId());
        booking.setPassengerName(request.passengerName());
        booking.setPassengerEmail(request.passengerEmail());
        booking.setSeatsBooked(request.seats());
        booking.setTotalPrice(totalPrice);
        booking.setStatus(BookingStatus.CONFIRMED);

        Booking saved = bookingRepository.save(booking);

        // Publish the event AFTER the booking is safely persisted.
        // (Note: this isn't transactional with the DB write yet — if publishing fails the
        //  booking is still saved. The "transactional outbox" pattern fixes that; a later lesson.)
        eventPublisher.publishBookingCreated(BookingCreatedEvent.from(saved));

        return saved;
    }

    public Booking getByReference(String reference) {
        return bookingRepository.findByBookingReference(reference)
                .orElseThrow(() -> new NotFoundException("Booking not found: " + reference));
    }
}
