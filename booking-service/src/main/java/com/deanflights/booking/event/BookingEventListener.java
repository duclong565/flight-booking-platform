package com.deanflights.booking.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * The CONSUMER side. The @KafkaListener method runs automatically whenever a message lands
 * on the topic — Spring polls Kafka in a background thread and hands us the deserialized event.
 *
 * <p>The consumer group comes from spring.kafka.consumer.group-id (application.properties).
 * For now we just log; in a real system this is where you'd send a confirmation email,
 * update a search index, kick off the next workflow step, etc.
 */
@Component
public class BookingEventListener {

    private static final Logger log = LoggerFactory.getLogger(BookingEventListener.class);

    // autoStartup=false (via app.events.enabled) lets the app run with no broker attached.
    @KafkaListener(topics = BookingCreatedEvent.TOPIC, autoStartup = "${app.events.enabled:true}")
    public void onBookingCreated(BookingCreatedEvent event) {
        log.info("✉️ Confirmation sent to {} for booking {} ({} seats, total {})",
                event.passengerEmail(), event.bookingReference(),
                event.seatsBooked(), event.totalPrice());
    }
}
