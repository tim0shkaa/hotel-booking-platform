package edu.hotel.payment.kafka;

import edu.hotel.common.model.KafkaTopics;
import edu.hotel.events.PaymentConfirmedEvent;
import edu.hotel.events.PaymentFailedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendPaymentConfirmed(PaymentConfirmedEvent event) {
        kafkaTemplate.send(KafkaTopics.PAYMENT_CONFIRMED,
                String.valueOf(event.getPaymentId()), event);
    }

    public void sendPaymentFailed(PaymentFailedEvent event) {
        kafkaTemplate.send(KafkaTopics.PAYMENT_FAILED,
                String.valueOf(event.getPaymentId()), event);
    }
}
