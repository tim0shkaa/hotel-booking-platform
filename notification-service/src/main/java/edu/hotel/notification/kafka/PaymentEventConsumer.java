package edu.hotel.notification.kafka;

import edu.hotel.common.model.KafkaTopics;
import edu.hotel.events.PaymentConfirmedEvent;
import edu.hotel.events.PaymentFailedEvent;
import edu.hotel.notification.entity.ProcessedEvent;
import edu.hotel.notification.repository.ProcessedEventRepository;
import edu.hotel.notification.service.NotificationService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentEventConsumer {

    private final ProcessedEventRepository processedEventRepository;

    private final NotificationService notificationService;

    @Transactional
    @KafkaListener(topics = KafkaTopics.PAYMENT_CONFIRMED, groupId = "notification-service-group")
    public void handlePaymentConfirmed(PaymentConfirmedEvent event) {

        if (processedEventRepository.existsByEventId(event.getEventId())) {
            return;
        }

        notificationService.handlePaymentConfirmed(event);

        ProcessedEvent processedEvent = new ProcessedEvent();
        processedEvent.setEventId(event.getEventId());

        processedEventRepository.save(processedEvent);
    }

    @Transactional
    @KafkaListener(topics = KafkaTopics.PAYMENT_FAILED, groupId = "notification-service-group")
    public void handlePaymentFailed(PaymentFailedEvent event) {

        if (processedEventRepository.existsByEventId(event.getEventId())) {
            return;
        }

        notificationService.handlePaymentFailed(event);

        ProcessedEvent processedEvent = new ProcessedEvent();
        processedEvent.setEventId(event.getEventId());

        processedEventRepository.save(processedEvent);
    }
}
