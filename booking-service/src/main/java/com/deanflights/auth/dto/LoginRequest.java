package com.deanflights.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Input shape for logging in. The annotations are checked by @Valid in the controller;
 * a violation makes Spring return HTTP 400 automatically.
 */
public record LoginRequest(
        @NotBlank String username,
        @NotBlank String password
) {}
