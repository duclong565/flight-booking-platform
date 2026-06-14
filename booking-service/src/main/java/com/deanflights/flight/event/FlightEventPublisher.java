package com.deanflights.flight.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    public FlightEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Send a FlightCreated event. We use the flight number as the message KEY: Kafka routes
     * all messages with the same key to the same partition, which preserves their order.
     */
    public void publishFlightCreated(FlightCreatedEvent event) {
        log.info("📤 Publishing FlightCreated to topic '{}': {}", FlightCreatedEvent.TOPIC, event);
        kafkaTemplate.send(FlightCreatedEvent.TOPIC, event.flightNumber(), event);
    }
}
