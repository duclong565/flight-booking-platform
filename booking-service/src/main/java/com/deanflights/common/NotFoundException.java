package com.deanflights.common;

/**
 * Thrown when a requested resource does not exist. The GlobalExceptionHandler maps it to
 * HTTP 404. Domain code throws meaning (not-found), not transport (HttpStatus).
 */
public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
}
