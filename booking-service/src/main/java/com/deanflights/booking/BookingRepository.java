package com.deanflights.booking;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    /** Look a booking up by its public, customer-facing reference (e.g. "BK-1A2B3C4D"). */
    Optional<Booking> findByBookingReference(String bookingReference);
}
