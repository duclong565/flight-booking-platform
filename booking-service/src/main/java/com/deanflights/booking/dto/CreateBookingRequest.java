package com.deanflights.booking.dto;

import jakarta.validation.constraints.*;

/**
 * Input shape for creating a booking. The annotations are checked by @Valid in the
 * controller; a violation makes Spring return HTTP 400 automatically.
 * Note there is no totalPrice here — the service derives it from the flight's base price.
 */
public record CreateBookingRequest(
        @NotNull Long flightId,
        @NotBlank String passengerName,
        @NotBlank @Email String passengerEmail,
        @Min(1) int seats
) {}
