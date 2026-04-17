package edu.hotel.review.service;

import edu.hotel.review.dto.rating.RatingAggregateResponse;
import edu.hotel.review.entity.Review;

public interface RatingAggregateService {

    // Вызывается из ReviewService
    void updateRating(Review review);

    // GET /hotels/{hotelId}/rating
    RatingAggregateResponse ratingByHotelId(Long hotelId);
}
