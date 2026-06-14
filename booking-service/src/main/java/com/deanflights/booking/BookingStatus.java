package com.deanflights.booking;

/**
 * Lifecycle state of a booking. Stored as a string in the DB (@Enumerated(EnumType.STRING))
 * so the column stays readable and is robust to reordering.
 */
public enum BookingStatus {
    CONFIRMED,
    CANCELLED
}
