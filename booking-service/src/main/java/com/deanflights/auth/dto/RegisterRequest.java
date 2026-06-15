package com.deanflights.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Input shape for registering a new account. The annotations are checked by @Valid in the
 * controller; a violation makes Spring return HTTP 400 automatically. The new user always
 * gets role USER — privilege is never client-supplied.
 */
public record RegisterRequest(
        @NotBlank String username,
        @NotBlank @Size(min = 8) String password
) {}
