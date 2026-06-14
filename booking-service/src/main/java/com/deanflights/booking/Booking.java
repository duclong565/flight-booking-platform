package com.deanflights.booking;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * A customer's booking on a flight. This is the persistence shape (the DB row) — it is never
 * exposed directly over HTTP; the controller maps it to/from DTO records.
 * The table is created automatically by Hibernate (ddl-auto=update) from these fields.
 */
@Entity
@Table(name = "bookings")
@Getter
@Setter
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "booking_reference", nullable = false, unique = true)
    private String bookingReference;

    // We reference the flight BY ID, not via a JPA @ManyToOne association. This is deliberate:
    // it keeps the Booking aggregate decoupled from Flight so bookings can later move to a
    // separate service without dragging a foreign-key relationship along.
    @Column(name = "flight_id", nullable = false)
    private Long flightId;

    @Column(nullable = false)
    private String passengerName;

    @Column(nullable = false)
    private String passengerEmail;

    @Column(name = "seats_booked")
    private int seatsBooked;

    @Column(name = "total_price")
    private BigDecimal totalPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
