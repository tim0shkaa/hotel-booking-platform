package edu.hotel.booking.kafka;

import edu.hotel.common.model.KafkaTopics;
import edu.hotel.events.BookingCancelledEvent;
import edu.hotel.events.BookingCompletedEvent;
import edu.hotel.events.BookingCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class BookingEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendBookingCreated(BookingCreatedEvent event) {
        kafkaTemplate.send(KafkaTopics.BOOKING_CREATED,
                String.valueOf(event.getBookingId()), event);
    }

    public void sendBookingCancelled(BookingCancelledEvent event) {
        kafkaTemplate.send(KafkaTopics.BOOKING_CANCELLED,
                String.valueOf(event.getBookingId()), event);
    }

    public void sendBookingCompleted(BookingCompletedEvent event) {
        kafkaTemplate.send(KafkaTopics.BOOKING_COMPLETED,
                String.valueOf(event.getBookingId()), event);
    }
}
