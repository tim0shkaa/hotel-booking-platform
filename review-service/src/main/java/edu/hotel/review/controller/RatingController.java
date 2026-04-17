package edu.hotel.review.controller;

import edu.hotel.review.dto.rating.RatingAggregateResponse;
import edu.hotel.review.dto.review.ReviewResponse;
import edu.hotel.review.service.RatingAggregateService;
import edu.hotel.review.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class RatingController {

    private final RatingAggregateService ratingAggregateService;
    private final ReviewService reviewService;

    @GetMapping("/hotels/{hotelId}/reviews")
    public ResponseEntity<Page<ReviewResponse>> findByHotelId(
            @PathVariable Long hotelId,
            Pageable pageable
    ) {
        return ResponseEntity.ok(reviewService.findByHotelId(hotelId, pageable));
    }

    @GetMapping("/room-types/{id}/reviews")
    public ResponseEntity<Page<ReviewResponse>> findByRoomTypeId(
            @PathVariable Long id,
            Pageable pageable
    ) {
        return ResponseEntity.ok(reviewService.findByRoomTypeId(id, pageable));
    }

    @GetMapping("/hotels/{hotelId}/rating")
    public ResponseEntity<RatingAggregateResponse> ratingByHotelId(@PathVariable Long hotelId) {
        return ResponseEntity.ok(ratingAggregateService.ratingByHotelId(hotelId));
    }
}
