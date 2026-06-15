package com.deanflights.flight.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * The PRODUCER side. Wraps Spring's KafkaTemplate so the rest of the app publishes events
 * through a tiny, intention-revealing method instead of touching Kafka APIs directly.
 *
 * <p>KafkaTemplate is auto-configured by Spring Boot from the spring.kafka.* properties
 * (String key serializer + JSON value serializer).
 */
@Component
public class FlightEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(FlightEventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    // Lets us run with no broker (e.g. a free-tier deploy): set app.events.enabled=false.
    private final boolean eventsEnabled;

    public FlightEventPublisher(KafkaTemplate<String, Object> kafkaTemplate,
                                @Value("${app.events.enabled:true}") boolean eventsEnabled) {
        this.kafkaTemplate = kafkaTemplate;
        this.eventsEnabled = eventsEnabled;
    }

    /**
     * Send a FlightCreated event. We use the flight number as the message KEY: Kafka routes
     * all messages with the same key to the same partition, which preserves their order.
     * Publishing is best-effort: a broker hiccup must never break the HTTP request.
     */
    public void publishFlightCreated(FlightCreatedEvent event) {
        if (!eventsEnabled) {
            log.debug("Events disabled (app.events.enabled=false); skipping {}", FlightCreatedEvent.TOPIC);
            return;
        }
        try {
            log.info("📤 Publishing FlightCreated to topic '{}': {}", FlightCreatedEvent.TOPIC, event);
            kafkaTemplate.send(FlightCreatedEvent.TOPIC, event.flightNumber(), event);
        } catch (Exception e) {
            log.error("Failed to publish FlightCreated (continuing): {}", e.getMessage());
        }
    }
}
