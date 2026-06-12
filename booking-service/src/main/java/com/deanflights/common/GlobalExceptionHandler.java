package com.deanflights.common;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Maps exceptions to RFC 7807 Problem Details. Spring serializes the returned ProblemDetail
 * as `application/problem+json` and sets `instance` to the request path automatically.
 * See docs/api-conventions.md (§B, §C).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ProblemDetail handleNotFound(NotFoundException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        pd.setTitle("Resource not found");
        pd.setType(ProblemTypes.NOT_FOUND);
        return pd;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        // Field errors: single-field constraints (@NotBlank, @Future, ...).
        // Global errors: class-level constraints (@ValidFlightTimes from Task 3).
        // Collect BOTH, or the cross-field message is silently dropped.
        List<Map<String, String>> errors = new ArrayList<>();
        ex.getBindingResult().getFieldErrors().forEach(fe ->
                errors.add(Map.of(
                        "field", fe.getField(),
                        "message", fe.getDefaultMessage() == null ? "invalid" : fe.getDefaultMessage())));
        ex.getBindingResult().getGlobalErrors().forEach(ge ->
                errors.add(Map.of(
                        "field", ge.getObjectName(),
                        "message", ge.getDefaultMessage() == null ? "invalid" : ge.getDefaultMessage())));

        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Request contains %d invalid field(s)".formatted(errors.size()));
        pd.setTitle("Validation failed");
        pd.setType(ProblemTypes.VALIDATION_ERROR);
        pd.setProperty("errors", errors);   // RFC 7807 extension member
        return pd;
    }
}
