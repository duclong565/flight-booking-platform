package com.deanflights.flight.validation;

import com.deanflights.flight.dto.CreateFlightRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Enforces arrivalTime > departureTime. Returns true when either is null so that
 * @NotNull/@Future report the null/past cases — we don't double-report here.
 */
public class FlightTimesValidator implements ConstraintValidator<ValidFlightTimes, CreateFlightRequest> {

    @Override
    public boolean isValid(CreateFlightRequest req, ConstraintValidatorContext ctx) {
        if (req.departureTime() == null || req.arrivalTime() == null) {
            return true;
        }
        return req.arrivalTime().isAfter(req.departureTime());
    }
}
