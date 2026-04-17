package edu.hotel.review.kafka;

import edu.hotel.common.model.KafkaTopics;
import edu.hotel.events.ReviewCreatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReviewEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendReviewCreated(ReviewCreatedEvent event) {
        kafkaTemplate.send(KafkaTopics.REVIEW_CREATED,
                String.valueOf(event.getReviewId()), event);
    }
}
