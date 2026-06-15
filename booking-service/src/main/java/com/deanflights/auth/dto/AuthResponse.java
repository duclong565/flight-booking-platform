package com.deanflights.auth.dto;

/**
 * Output shape returned after a successful login. The client sends the token back on
 * subsequent requests as `Authorization: Bearer <token>`. expiresInMs lets the client
 * know how long the token stays valid without parsing it.
 */
public record AuthResponse(
        String token,
        String tokenType,
        long expiresInMs
) {}
