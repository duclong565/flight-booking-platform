package com.deanflights.booking.dto;

import com.deanflights.booking.Booking;
import com.deanflights.booking.BookingStatus;

import java.math.BigDecimal;

/**
 * Output shape returned to clients. The static from(...) factory does the manual
 * mapping from the entity — this is the seam that keeps our API independent of the
 * DB schema. (We adopt MapStruct later, when this hand-mapping gets repetitive.)
 */
public record BookingResponse(
        Long id,
        String bookingReference,
        Long flightId,
        String passengerName,
        String passengerEmail,
        int seatsBooked,
        BigDecimal totalPrice,
        BookingStatus status
) {
    public static BookingResponse from(Booking b) {
        return new BookingResponse(
                b.getId(), b.getBookingReference(), b.getFlightId(), b.getPassengerName(),
                b.getPassengerEmail(), b.getSeatsBooked(), b.getTotalPrice(), b.getStatus());
    }
}
