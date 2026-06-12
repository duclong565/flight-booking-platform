package com.deanflights.flight;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface FlightRepository extends JpaRepository<Flight, Long> {

    /**
     * Search with optional filters — a null parameter means "don't filter on this".
     * Pagination/sorting come from the Pageable argument.
     */
    // CAST(:x AS ...) gives the bound parameter an explicit SQL type. Without it, Postgres
    // sees an untyped null in `:x IS NULL` and fails with "could not determine data type".
    @Query("""
            SELECT f FROM Flight f
            WHERE (CAST(:origin AS string) IS NULL OR f.origin = :origin)
              AND (CAST(:destination AS string) IS NULL OR f.destination = :destination)
              AND (CAST(:from AS timestamp) IS NULL OR f.departureTime >= :from)
              AND (CAST(:to AS timestamp) IS NULL OR f.departureTime < :to)
            """)
    Page<Flight> search(@Param("origin") String origin,
                        @Param("destination") String destination,
                        @Param("from") Instant from,
                        @Param("to") Instant to,
                        Pageable pageable);
}
