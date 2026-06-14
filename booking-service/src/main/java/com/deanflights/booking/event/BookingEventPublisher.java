package com.deanflights.booking.event;

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
public class BookingEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(BookingEventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public BookingEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Send a BookingCreated event. We use the booking reference as the message KEY: Kafka routes
     * all messages with the same key to the same partition, which preserves their order.
     */
    public void publishBookingCreated(BookingCreatedEvent event) {
        log.info("📤 Publishing BookingCreated to topic '{}': {}", BookingCreatedEvent.TOPIC, event);
        kafkaTemplate.send(BookingCreatedEvent.TOPIC, event.bookingReference(), event);
    }
}
