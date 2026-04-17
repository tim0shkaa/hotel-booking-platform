package edu.hotel.review.dto.rating;

import edu.hotel.review.model.TargetType;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class RatingAggregateResponse {

    private Long targetId;

    private TargetType targetType;

    private Double avgRating;

    private Integer totalReviews;

    private String ratingDistribution;
}
