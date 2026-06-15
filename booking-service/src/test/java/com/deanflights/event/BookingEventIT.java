package com.deanflights.event;

import com.deanflights.AbstractIntegrationTest;
import com.deanflights.TestAuthSupport;
import com.deanflights.booking.BookingRepository;
import com.deanflights.booking.event.BookingCreatedEvent;
import com.deanflights.flight.FlightRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The "real Kafka" integration test. After creating a booking via the HTTP API, we assert that
 * a {@link BookingCreatedEvent} message actually lands on the broker on
 * {@link BookingCreatedEvent#TOPIC}.
 *
 * <p>Rather than relying on the application's own @KafkaListener, we register an INDEPENDENT
 * test {@link KafkaConsumer} (its own consumer group) subscribed to the topic, perform the
 * booking, and use Awaitility to poll until the message arrives. The broker address comes from
 * the running Testcontainers Kafka container — the same one the app's producer is wired to via
 * {@code @ServiceConnection} — which we read from the Spring-resolved
 * {@code spring.kafka.bootstrap-servers} property.
 */
class BookingEventIT extends AbstractIntegrationTest {

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    FlightRepository flightRepository;

    @Autowired
    BookingRepository bookingRepository;

    // Resolved by Spring Boot's @ServiceConnection from the Testcontainers Kafka container.
    @Value("${spring.kafka.bootstrap-servers}")
    String bootstrapServers;

    TestAuthSupport auth;

    @BeforeEach
    void setUp() {
        auth = new TestAuthSupport(mockMvc, objectMapper);
        bookingRepository.deleteAll();
        flightRepository.deleteAll();
    }

    private long createFlight(String flightNumber) throws Exception {
        Instant departure = Instant.now().plus(2, ChronoUnit.DAYS);
        Instant arrival = departure.plus(3, ChronoUnit.HOURS);
        String body = """
                {
                  "flightNumber": "%s",
                  "origin": "SGN",
                  "destination": "HAN",
                  "departureTime": "%s",
                  "arrivalTime": "%s",
                  "totalSeats": 100,
                  "basePrice": 100.00
                }
                """.formatted(flightNumber, departure, arrival);

        String response = mockMvc.perform(post("/api/v1/flights")
                        .header(HttpHeaders.AUTHORIZATION, auth.adminBearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    @Test
    void creatingABooking_publishesBookingCreatedEventToKafka() throws Exception {
        long flightId = createFlight("VN-900");
        String userBearer = auth.registerAndLoginFreshUser();

        // Start a test consumer BEFORE booking. The app produces with auto-offset-reset behaviour
        // we don't control on this consumer, so use "earliest" to be safe and a unique group so we
        // read from the start of the topic regardless of timing.
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "it-consumer-" + UUID.randomUUID());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        List<String> received = new CopyOnWriteArrayList<>();

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
            consumer.subscribe(List.of(BookingCreatedEvent.TOPIC));
            // One poll to force partition assignment before we produce.
            consumer.poll(Duration.ofMillis(500));

            // Perform the booking — this triggers the producer.
            String bookingBody = """
                    {
                      "flightId": %d,
                      "passengerName": "Dean Nguyen",
                      "passengerEmail": "dean@example.com",
                      "seats": 2
                    }
                    """.formatted(flightId);

            String bookingResponse = mockMvc.perform(post("/api/v1/bookings")
                            .header(HttpHeaders.AUTHORIZATION, userBearer)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(bookingBody))
                    .andExpect(status().isCreated())
                    .andReturn().getResponse().getContentAsString();

            String expectedReference = objectMapper.readTree(bookingResponse).get("bookingReference").asText();

            // Await the message landing on the topic (<= 15s).
            await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(500));
                for (ConsumerRecord<String, String> record : records) {
                    received.add(record.value());
                }
                assertThat(received)
                        .as("a BookingCreated message containing the booking reference should arrive")
                        .anySatisfy(payload -> assertThat(payload).contains(expectedReference));
            });

            // Sanity check: the payload deserializes and carries the right reference.
            String match = received.stream()
                    .filter(p -> p.contains(expectedReference))
                    .findFirst()
                    .orElseThrow();
            JsonNode node = objectMapper.readTree(match);
            assertThat(node.get("bookingReference").asText()).isEqualTo(expectedReference);
            assertThat(node.get("seatsBooked").asInt()).isEqualTo(2);
        }
    }
}
