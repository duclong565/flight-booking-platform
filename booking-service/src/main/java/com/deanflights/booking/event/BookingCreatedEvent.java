package com.deanflights.booking.event;

import com.deanflights.booking.Booking;

import java.math.BigDecimal;

/**
 * A domain event: "a booking was created". This is the message we publish to Kafka.
 *
 * <p>It is a small, immutable snapshot — NOT the JPA entity. We deliberately send only the
 * facts other parts of the system care about, so consumers don't depend on our DB shape.
 * (Same idea as a DTO, but for messaging instead of HTTP.)
 *
 * <p>Records serialize cleanly to/from JSON via Jackson, which is what Spring Kafka's
 * JsonSerializer/JsonDeserializer use.
 */
public record BookingCreatedEvent(
        String bookingReference,
        Long flightId,
        String passengerEmail,
        int seatsBooked,
        BigDecimal totalPrice
) {
    /** The Kafka topic this event is published to. Referenced by the publisher and listener. */
    public static final String TOPIC = "bookings.created";

    /** Build the event from a freshly-saved Booking entity. */
    public static BookingCreatedEvent from(Booking booking) {
        return new BookingCreatedEvent(
                booking.getBookingReference(),
                booking.getFlightId(),
                booking.getPassengerEmail(),
                booking.getSeatsBooked(),
                booking.getTotalPrice());
    }
}
