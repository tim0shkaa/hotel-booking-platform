package edu.hotel.payment.config;

import edu.hotel.common.model.KafkaTopics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic paymentConfirmedTopic() {
        return TopicBuilder.name(KafkaTopics.PAYMENT_CONFIRMED)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic paymentFailedTopic() {
        return TopicBuilder.name(KafkaTopics.PAYMENT_FAILED)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic paymentConfirmedDlq() {
        return TopicBuilder.name(KafkaTopics.PAYMENT_CONFIRMED_DLQ)
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic paymentFailedDlq() {
        return TopicBuilder.name(KafkaTopics.PAYMENT_FAILED_DLQ)
                .partitions(1)
                .replicas(1)
                .build();
    }
}
