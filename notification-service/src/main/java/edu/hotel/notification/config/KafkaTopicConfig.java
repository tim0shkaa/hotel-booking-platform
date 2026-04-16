package edu.hotel.notification.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic bookingCreatedDlq() {
        return TopicBuilder.name("booking.created.dlq")
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic bookingCancelledDlq() {
        return TopicBuilder.name("booking.cancelled.dlq")
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic bookingCompletedDlq() {
        return TopicBuilder.name("booking.completed.dlq")
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic paymentConfirmedDlq() {
        return TopicBuilder.name("payment.confirmed.dlq")
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic paymentFailedDlq() {
        return TopicBuilder.name("payment.failed.dlq")
                .partitions(1)
                .replicas(1)
                .build();
    }
}