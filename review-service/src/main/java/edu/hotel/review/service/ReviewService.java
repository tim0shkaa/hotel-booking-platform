package edu.hotel.review.service;

import edu.hotel.review.dto.review.ReviewRequest;
import edu.hotel.review.dto.review.ReviewResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ReviewService {

    // POST /reviews
    ReviewResponse createReview(ReviewRequest request, Long UserId);

    // GET /reviews/{id}
    ReviewResponse findById(Long id);

    // GET /hotels/{hotelId}/reviews
    Page<ReviewResponse> findByHotelId(Long hotelId, Pageable pageable);

    // GET /room-types/{id}/reviews
    Page<ReviewResponse> findByRoomTypeId(Long roomTypeId, Pageable pageable);

    // DELETE reviews/{id}
    void delete(Long id);
}
