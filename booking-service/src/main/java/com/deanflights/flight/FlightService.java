package com.deanflights.flight;

import com.deanflights.flight.dto.CreateFlightRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Business logic for flights. Kept deliberately thin in this slice: map the request
 * to an entity, set the starting seat count, and delegate persistence to the repository.
 */
@Service
public class FlightService {

    private final FlightRepository flightRepository;

    // Constructor injection — Spring passes the repository bean in automatically.
    public FlightService(FlightRepository flightRepository) {
        this.flightRepository = flightRepository;
    }

    public Flight create(CreateFlightRequest request) {
        Flight flight = new Flight();
        flight.setFlightNumber(request.flightNumber());
        flight.setOrigin(request.origin());
        flight.setDestination(request.destination());
        flight.setDepartureTime(request.departureTime());
        flight.setArrivalTime(request.arrivalTime());
        flight.setTotalSeats(request.totalSeats());
        flight.setAvailableSeats(request.totalSeats()); // a brand-new flight starts full
        flight.setBasePrice(request.basePrice());
        return flightRepository.save(flight);
    }

    public Flight getById(Long id) {
        return flightRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Flight not found: " + id));
    }

    public List<Flight> findAll() {
        return flightRepository.findAll();
    }
}
