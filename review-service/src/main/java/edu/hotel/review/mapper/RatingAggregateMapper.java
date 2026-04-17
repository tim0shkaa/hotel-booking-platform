package edu.hotel.review.mapper;

import edu.hotel.review.dto.rating.RatingAggregateResponse;
import edu.hotel.review.entity.RatingAggregate;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface RatingAggregateMapper {

    RatingAggregateResponse toResponse(RatingAggregate ratingAggregate);
}
