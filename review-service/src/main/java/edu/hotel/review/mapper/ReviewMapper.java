package edu.hotel.review.mapper;

import edu.hotel.review.dto.review.ReviewRequest;
import edu.hotel.review.dto.review.ReviewResponse;
import edu.hotel.review.entity.Review;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ReviewMapper {

    Review toEntity(ReviewRequest request);

    ReviewResponse toResponse(Review review);
}
