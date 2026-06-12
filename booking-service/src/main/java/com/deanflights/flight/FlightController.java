package com.deanflights.flight;

import com.deanflights.flight.dto.CreateFlightRequest;
import com.deanflights.flight.dto.FlightResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * HTTP layer for flights. Only concerns: routing, status codes, JSON in/out, and
 * triggering validation (@Valid). It never returns the entity directly — always a DTO.
 */
@RestController
@RequestMapping("/api/v1/flights")
public class FlightController {

    private final FlightService flightService;

    public FlightController(FlightService flightService) {
        this.flightService = flightService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FlightResponse create(@Valid @RequestBody CreateFlightRequest request) {
        return FlightResponse.from(flightService.create(request));
    }

    @GetMapping("/{id}")
    public FlightResponse getById(@PathVariable Long id) {
        return FlightResponse.from(flightService.getById(id));
    }

    @GetMapping
    public Page<FlightResponse> search(
            @RequestParam(required = false) String origin,
            @RequestParam(required = false) String destination,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate departureDate,
            Pageable pageable) {
        return flightService.search(origin, destination, departureDate, pageable)
                .map(FlightResponse::from);
    }
}
