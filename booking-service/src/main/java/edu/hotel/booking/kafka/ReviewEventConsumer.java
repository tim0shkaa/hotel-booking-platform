package edu.hotel.booking.kafka;

import edu.hotel.booking.entity.Hotel;
import edu.hotel.booking.entity.ProcessedEvent;
import edu.hotel.booking.repository.HotelRepository;
import edu.hotel.booking.repository.ProcessedEventRepository;
import edu.hotel.common.exception.NotFoundException;
import edu.hotel.common.model.KafkaTopics;
import edu.hotel.events.ReviewCreatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class ReviewEventConsumer {

    private final ProcessedEventRepository processedEventRepository;

    private final HotelRepository hotelRepository;

    @Transactional
    @KafkaListener(topics = KafkaTopics.REVIEW_CREATED, groupId = "booking-service-group")
    public void handleReviewCreated(ReviewCreatedEvent event) {

        if (processedEventRepository.existsByEventId(event.getEventId())) {
            return;
        }

        Hotel hotel = hotelRepository.findById(event.getHotelId())
                .orElseThrow(() -> new NotFoundException("Отеля с id: " + event.getEventId() + " не существует"));

        double currentAvg = hotel.getAvgRating() != null ? hotel.getAvgRating() : 0.0;
        double newAvg = (currentAvg * hotel.getTotalReviews() + event.getOverallRating())
                / (hotel.getTotalReviews() + 1);

        hotel.setAvgRating(newAvg);

        hotelRepository.save(hotel);

        ProcessedEvent processedEvent = new ProcessedEvent();
        processedEvent.setEventId(event.getEventId());

        processedEventRepository.save(processedEvent);
    }
}
