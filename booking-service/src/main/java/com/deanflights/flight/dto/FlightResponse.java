package com.deanflights.flight.dto;

import com.deanflights.flight.Flight;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Output shape returned to clients. The static from(...) factory does the manual
 * mapping from the entity — this is the seam that keeps our API independent of the
 * DB schema. (We adopt MapStruct later, when this hand-mapping gets repetitive.)
 */
public record FlightResponse(
        Long id,
        String flightNumber,
        String origin,
        String destination,
        Instant departureTime,
        Instant arrivalTime,
        int totalSeats,
        int availableSeats,
        BigDecimal basePrice
) {
    public static FlightResponse from(Flight f) {
        return new FlightResponse(
                f.getId(), f.getFlightNumber(), f.getOrigin(), f.getDestination(),
                f.getDepartureTime(), f.getArrivalTime(), f.getTotalSeats(),
                f.getAvailableSeats(), f.getBasePrice());
    }
}
