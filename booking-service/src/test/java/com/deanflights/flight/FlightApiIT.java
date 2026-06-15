package com.deanflights.flight;

import com.deanflights.AbstractIntegrationTest;
import com.deanflights.TestAuthSupport;
import com.deanflights.booking.BookingRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for the flight HTTP API, run against real Postgres + Kafka and the real
 * JWT security filter chain.
 */
class FlightApiIT extends AbstractIntegrationTest {

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    FlightRepository flightRepository;

    @Autowired
    BookingRepository bookingRepository;

    TestAuthSupport auth;

    @BeforeEach
    void setUp() {
        auth = new TestAuthSupport(mockMvc, objectMapper);
        // Bookings reference flights, so delete bookings first to avoid leftover state colliding.
        bookingRepository.deleteAll();
        flightRepository.deleteAll();
    }

    /** A valid create-flight request body with a future departure before a future arrival. */
    private String validFlightBody(String flightNumber) {
        Instant departure = Instant.now().plus(2, ChronoUnit.DAYS);
        Instant arrival = departure.plus(3, ChronoUnit.HOURS);
        return """
                {
                  "flightNumber": "%s",
                  "origin": "SGN",
                  "destination": "HAN",
                  "departureTime": "%s",
                  "arrivalTime": "%s",
                  "totalSeats": 180,
                  "basePrice": 99.99
                }
                """.formatted(flightNumber, departure, arrival);
    }

    @Test
    void publicSearch_withNoAuth_returns200() throws Exception {
        mockMvc.perform(get("/api/v1/flights"))
                .andExpect(status().isOk());
    }

    @Test
    void createFlight_asAdmin_returns201() throws Exception {
        mockMvc.perform(post("/api/v1/flights")
                        .header(HttpHeaders.AUTHORIZATION, auth.adminBearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validFlightBody("VN-100")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.flightNumber").value("VN-100"))
                .andExpect(jsonPath("$.availableSeats").value(180));
    }

    @Test
    void createFlight_asUser_returns403_problemJson() throws Exception {
        mockMvc.perform(post("/api/v1/flights")
                        .header(HttpHeaders.AUTHORIZATION, auth.registerAndLoginFreshUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validFlightBody("VN-200")))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void createFlight_withNoToken_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/flights")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validFlightBody("VN-300")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createFlight_withBlankFlightNumber_returns400() throws Exception {
        Instant departure = Instant.now().plus(2, ChronoUnit.DAYS);
        Instant arrival = departure.plus(3, ChronoUnit.HOURS);
        String body = """
                {
                  "flightNumber": "",
                  "origin": "SGN",
                  "destination": "HAN",
                  "departureTime": "%s",
                  "arrivalTime": "%s",
                  "totalSeats": 180,
                  "basePrice": 99.99
                }
                """.formatted(departure, arrival);

        mockMvc.perform(post("/api/v1/flights")
                        .header(HttpHeaders.AUTHORIZATION, auth.adminBearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void createFlight_withPastDeparture_returns400() throws Exception {
        Instant pastDeparture = Instant.now().minus(2, ChronoUnit.DAYS);
        Instant arrival = Instant.now().plus(2, ChronoUnit.DAYS);
        String body = """
                {
                  "flightNumber": "VN-400",
                  "origin": "SGN",
                  "destination": "HAN",
                  "departureTime": "%s",
                  "arrivalTime": "%s",
                  "totalSeats": 180,
                  "basePrice": 99.99
                }
                """.formatted(pastDeparture, arrival);

        mockMvc.perform(post("/api/v1/flights")
                        .header(HttpHeaders.AUTHORIZATION, auth.adminBearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }
}
