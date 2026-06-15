package com.deanflights.auth;

/**
 * A user's authorization level. Stored as a string in the DB (@Enumerated(STRING)) and
 * mapped to a Spring Security authority "ROLE_USER" / "ROLE_ADMIN" at authentication time.
 */
public enum Role {
    USER,
    ADMIN
}
