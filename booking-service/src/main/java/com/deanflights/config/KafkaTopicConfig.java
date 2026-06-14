package com.deanflights.config;

import com.deanflights.flight.event.FlightCreatedEvent;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Declares the Kafka topics this app owns. Spring's KafkaAdmin sees these NewTopic beans
 * on startup and creates the topics automatically if they don't exist — so we don't rely
 * on Kafka's auto-create behaviour.
 *
 * <p>1 partition + 1 replica is correct for a single-node local broker. In production you'd
 * use more partitions (parallelism) and replicas > 1 (durability).
 */
@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic flightsCreatedTopic() {
        return TopicBuilder.name(FlightCreatedEvent.TOPIC)
                .partitions(1)
                .replicas(1)
                .build();
    }
}
