package edu.hotel.review.service;

import edu.hotel.common.exception.AlreadyExistsException;
import edu.hotel.common.exception.NotFoundException;
import edu.hotel.common.model.KafkaTopics;
import edu.hotel.events.ReviewCreatedEvent;
import edu.hotel.review.dto.rating.RatingAggregateResponse;
import edu.hotel.review.dto.reply.ReviewReplyRequest;
import edu.hotel.review.dto.reply.ReviewReplyResponse;
import edu.hotel.review.dto.review.ReviewRequest;
import edu.hotel.review.dto.review.ReviewResponse;
import edu.hotel.review.entity.EligibleBooking;
import edu.hotel.review.entity.Review;
import edu.hotel.review.exception.BookingNotEligibleForReviewException;
import edu.hotel.review.kafka.ReviewEventProducer;
import edu.hotel.review.mapper.ReviewMapper;
import edu.hotel.review.repository.EligibleBookingRepository;
import edu.hotel.review.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final EligibleBookingRepository eligibleBookingRepository;

    private final ReviewRepository reviewRepository;

    private final ReviewMapper reviewMapper;

    private final ReviewEventProducer reviewEventProducer;

    private final RatingAggregateService ratingAggregateService;

    @Override
    @Transactional
    public ReviewResponse createReview(ReviewRequest request, Long userId) {

        EligibleBooking eligibleBooking = eligibleBookingRepository.findByBookingId(request.getBookingId())
                .orElseThrow(() -> new NotFoundException("Отзыв на данное пока бронирование нельзя оставить"));

        if (!eligibleBooking.getUserId().equals(userId)) {
            throw new BookingNotEligibleForReviewException("Нельзя оставить отзыв");
        }

        if (reviewRepository.existsByBookingId(request.getBookingId())) {
            throw new AlreadyExistsException(
                    "Отзыв на бронирование с id: " + request.getBookingId() + " уже существует");
        }

        Review review = reviewMapper.toEntity(request);
        review.setHotelId(eligibleBooking.getHotelId());
        review.setGuestId(eligibleBooking.getGuestId());
        review.setRoomTypeId(eligibleBooking.getRoomTypeId());

        Review savedReview = reviewRepository.save(review);

        ratingAggregateService.updateRating(savedReview);

        ReviewCreatedEvent event = ReviewCreatedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType(KafkaTopics.REVIEW_CREATED)
                .reviewId(savedReview.getId())
                .hotelId(savedReview.getHotelId())
                .roomTypeId(savedReview.getRoomTypeId())
                .overallRating(savedReview.getOverallRating())
                .occurredAt(LocalDateTime.now())
                .build();

        reviewEventProducer.sendReviewCreated(event);

        return reviewMapper.toResponse(savedReview);
    }

    @Override
    @Transactional(readOnly = true)
    public ReviewResponse findById(Long id) {

        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Отзыва с id: " + id + " не существует"));

        return reviewMapper.toResponse(review);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReviewResponse> findByHotelId(Long hotelId, Pageable pageable) {

        Page<Review> reviews = reviewRepository.findAllByHotelId(hotelId, pageable);

        return reviews.map(reviewMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReviewResponse> findByRoomTypeId(Long roomTypeId, Pageable pageable) {

        Page<Review> reviews = reviewRepository.findAllByRoomTypeId(roomTypeId, pageable);

        return reviews.map(reviewMapper::toResponse);
    }

    @Override
    @Transactional
    public void delete(Long id) {

        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Отзыва с id: " + id + " не существует"));

        reviewRepository.delete(review);
    }
}
