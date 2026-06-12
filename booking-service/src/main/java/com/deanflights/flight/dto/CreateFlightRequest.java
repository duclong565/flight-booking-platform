package com.deanflights.flight.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Input shape for creating a flight. The annotations are checked by @Valid in the
 * controller; a violation makes Spring return HTTP 400 automatically.
 * Note there is no availableSeats here — the service derives it from totalSeats.
 */
public record CreateFlightRequest(
        @NotBlank String flightNumber,
        @NotBlank String origin,
        @NotBlank String destination,
        @NotNull @Future Instant departureTime,
        @NotNull @Future Instant arrivalTime,
        @Min(1) int totalSeats,
        @NotNull @DecimalMin("0.0") BigDecimal basePrice
) {}
