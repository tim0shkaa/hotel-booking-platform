package edu.hotel.notification.kafka;

import edu.hotel.common.model.KafkaTopics;
import edu.hotel.events.BookingCancelledEvent;
import edu.hotel.events.BookingCompletedEvent;
import edu.hotel.events.BookingCreatedEvent;
import edu.hotel.notification.entity.ProcessedEvent;
import edu.hotel.notification.repository.ProcessedEventRepository;
import edu.hotel.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class BookingEventConsumer {

    private final ProcessedEventRepository processedEventRepository;

    private final NotificationService notificationService;

    @Transactional
    @KafkaListener(topics = KafkaTopics.BOOKING_CREATED, groupId = "notification-service-group")
    public void handleBookingCreated(BookingCreatedEvent event) {

        if (processedEventRepository.existsByEventId(event.getEventId())) {
            return;
        }

        notificationService.handleBookingCreated(event);

        ProcessedEvent processedEvent = new ProcessedEvent();
        processedEvent.setEventId(event.getEventId());

        processedEventRepository.save(processedEvent);
    }

    @Transactional
    @KafkaListener(topics = KafkaTopics.BOOKING_CANCELLED, groupId = "notification-service-group")
    public void handleBookingCancelled(BookingCancelledEvent event) {

        if (processedEventRepository.existsByEventId(event.getEventId())) {
            return;
        }

        notificationService.handleBookingCancelled(event);

        ProcessedEvent processedEvent = new ProcessedEvent();
        processedEvent.setEventId(event.getEventId());

        processedEventRepository.save(processedEvent);
    }

    @Transactional
    @KafkaListener(topics = KafkaTopics.BOOKING_COMPLETED, groupId = "notification-service-group")
    public void handleBookingCompleted(BookingCompletedEvent event) {

        if (processedEventRepository.existsByEventId(event.getEventId())) {
            return;
        }

        notificationService.handleBookingCompleted(event);

        ProcessedEvent processedEvent = new ProcessedEvent();
        processedEvent.setEventId(event.getEventId());

        processedEventRepository.save(processedEvent);
    }
}
