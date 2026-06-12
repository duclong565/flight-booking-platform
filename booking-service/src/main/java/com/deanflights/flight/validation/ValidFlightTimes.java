package com.deanflights.flight.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Class-level constraint: a flight's arrivalTime must be after its departureTime.
 * Applied to a whole object (TYPE) because it spans two fields.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = FlightTimesValidator.class)
@Documented
public @interface ValidFlightTimes {
    String message() default "arrivalTime must be after departureTime";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
