package com.deanflights.common;

import java.net.URI;

/**
 * Stable RFC 7807 problem `type` URIs, shared across services so error identities stay
 * consistent. These are identifiers — they need not resolve to a live page.
 */
public final class ProblemTypes {

    private static final String BASE = "https://api.flightbooking.dev/problems/";

    public static final URI NOT_FOUND        = URI.create(BASE + "not-found");
    public static final URI VALIDATION_ERROR = URI.create(BASE + "validation-error");

    private ProblemTypes() {}
}
