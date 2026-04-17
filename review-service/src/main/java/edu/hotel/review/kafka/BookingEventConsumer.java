package edu.hotel.review.kafka;

import edu.hotel.common.model.KafkaTopics;
import edu.hotel.events.BookingCompletedEvent;
import edu.hotel.review.entity.EligibleBooking;
import edu.hotel.review.entity.ProcessedEvent;
import edu.hotel.review.repository.EligibleBookingRepository;
import edu.hotel.review.repository.ProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class BookingEventConsumer {

    private final ProcessedEventRepository processedEventRepository;

    private final EligibleBookingRepository eligibleBookingRepository;

    @Transactional
    @KafkaListener(topics = KafkaTopics.BOOKING_COMPLETED, groupId = "review-service-group")
    public void handleBookingCompleted(BookingCompletedEvent event) {

        if (processedEventRepository.existsByEventId(event.getEventId())) {
            return;
        }

        EligibleBooking eligibleBooking = new EligibleBooking();
        eligibleBooking.setBookingId(event.getBookingId());
        eligibleBooking.setGuestId(event.getGuestId());
        eligibleBooking.setHotelId(event.getHotelId());
        eligibleBooking.setRoomTypeId(event.getRoomTypeId());
        eligibleBooking.setUserId(event.getUserId());

        eligibleBookingRepository.save(eligibleBooking);

        ProcessedEvent processedEvent = new ProcessedEvent();
        processedEvent.setEventId(event.getEventId());
        processedEventRepository.save(processedEvent);
    }
}
