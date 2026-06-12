package com.deanflights.flight;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA generates the implementation at runtime. Extending JpaRepository
 * gives us save(), findById(), findAll(), deleteAll(), etc. for free.
 */
public interface FlightRepository extends JpaRepository<Flight, Long> {
}
