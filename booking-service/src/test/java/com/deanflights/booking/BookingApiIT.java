package com.deanflights.booking;

import com.deanflights.AbstractIntegrationTest;
import com.deanflights.TestAuthSupport;
import com.deanflights.flight.Flight;
import com.deanflights.flight.FlightRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for the booking HTTP API. A real flight is created (as admin), then booked
 * (as a fresh user) so the cross-feature seat-decrement + price calculation are exercised
 * against real Postgres.
 */
class BookingApiIT extends AbstractIntegrationTest {

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
        bookingRepository.deleteAll();
        flightRepository.deleteAll();
    }

    /** Create a flight via the admin API and return its generated id. */
    private long createFlight(String flightNumber, int totalSeats, String basePrice) throws Exception {
        Instant departure = Instant.now().plus(2, ChronoUnit.DAYS);
        Instant arrival = departure.plus(3, ChronoUnit.HOURS);
        String body = """
                {
                  "flightNumber": "%s",
                  "origin": "SGN",
                  "destination": "HAN",
                  "departureTime": "%s",
                  "arrivalTime": "%s",
                  "totalSeats": %d,
                  "basePrice": %s
                }
                """.formatted(flightNumber, departure, arrival, totalSeats, basePrice);

        String response = mockMvc.perform(post("/api/v1/flights")
                        .header(HttpHeaders.AUTHORIZATION, auth.adminBearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        JsonNode node = objectMapper.readTree(response);
        return node.get("id").asLong();
    }

    private String bookingBody(long flightId, int seats) {
        return """
                {
                  "flightId": %d,
                  "passengerName": "Dean Nguyen",
                  "passengerEmail": "dean@example.com",
                  "seats": %d
                }
                """.formatted(flightId, seats);
    }

    @Test
    void user_canBookFlight_returns201_withTotalPrice_andDecrementsSeats() throws Exception {
        long flightId = createFlight("VN-500", 100, "120.00");
        int seats = 3;

        String userBearer = auth.registerAndLoginFreshUser();

        mockMvc.perform(post("/api/v1/bookings")
                        .header(HttpHeaders.AUTHORIZATION, userBearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingBody(flightId, seats)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.bookingReference").isNotEmpty())
                .andExpect(jsonPath("$.seatsBooked").value(seats))
                // total price = basePrice (120.00) * seats (3) = 360.00
                .andExpect(jsonPath("$.totalPrice").value(360.00))
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        // availableSeats decremented from 100 -> 97
        Flight flight = flightRepository.findById(flightId).orElseThrow();
        assertThat(flight.getAvailableSeats()).isEqualTo(97);
        assertThat(flight.getBasePrice()).isEqualByComparingTo(new BigDecimal("120.00"));
    }

    @Test
    void overBooking_moreSeatsThanAvailable_returns422_problemJson() throws Exception {
        long flightId = createFlight("VN-600", 2, "50.00");

        String userBearer = auth.registerAndLoginFreshUser();

        mockMvc.perform(post("/api/v1/bookings")
                        .header(HttpHeaders.AUTHORIZATION, userBearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingBody(flightId, 5)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));

        // No seats were taken — the rejected booking must not have decremented inventory.
        Flight flight = flightRepository.findById(flightId).orElseThrow();
        assertThat(flight.getAvailableSeats()).isEqualTo(2);
        assertThat(bookingRepository.count()).isZero();
    }

    @Test
    void booking_withNoToken_returns401() throws Exception {
        long flightId = createFlight("VN-700", 50, "75.00");

        mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingBody(flightId, 1)))
                .andExpect(status().isUnauthorized());
    }
}
