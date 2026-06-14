package com.deanflights.common;

/**
 * Thrown when a request is well-formed but violates a business rule (e.g. not enough seats).
 * The GlobalExceptionHandler maps it to HTTP 422. Domain code throws meaning (rule broken),
 * not transport (HttpStatus).
 */
public class BusinessRuleException extends RuntimeException {
    public BusinessRuleException(String message) {
        super(message);
    }
}
