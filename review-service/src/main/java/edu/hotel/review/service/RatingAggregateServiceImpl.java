package edu.hotel.review.service;

import edu.hotel.review.dto.rating.RatingAggregateResponse;
import edu.hotel.review.entity.RatingAggregate;
import edu.hotel.review.entity.Review;
import edu.hotel.review.mapper.RatingAggregateMapper;
import edu.hotel.review.model.TargetType;
import edu.hotel.review.repository.RatingAggregateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class RatingAggregateServiceImpl implements RatingAggregateService {

    private final RatingAggregateRepository ratingAggregateRepository;

    private final RatingAggregateMapper ratingAggregateMapper;

    @Override
    @Transactional
    public void updateRating(Review review) {

        ratingAggregateRepository.save(setRatingAggregate(TargetType.HOTEL, review));

        ratingAggregateRepository.save(setRatingAggregate(TargetType.ROOM_TYPE, review));
    }

    @Override
    @Transactional(readOnly = true)
    public RatingAggregateResponse ratingByHotelId(Long hotelId) {

        RatingAggregate ratingAggregate = ratingAggregateRepository
                .findByTargetTypeAndTargetId(TargetType.HOTEL, hotelId)
                .orElseGet(() -> {
                    RatingAggregate newAggregate = new RatingAggregate();
                    newAggregate.setTargetType(TargetType.HOTEL);
                    newAggregate.setTargetId(hotelId);
                    newAggregate.setTotalReviews(0);
                    newAggregate.setAvgRating(0.0);
                    return newAggregate;
                });

        return ratingAggregateMapper.toResponse(ratingAggregate);
    }

    private RatingAggregate setRatingAggregate(TargetType type, Review review) {

        Long targetId = type == TargetType.HOTEL ? review.getHotelId() : review.getRoomTypeId();

        RatingAggregate ratingAggregate = ratingAggregateRepository
                .findByTargetTypeAndTargetId(type, review.getHotelId())
                .orElseGet(() -> {
                    RatingAggregate newAggregate = new RatingAggregate();
                    newAggregate.setTargetType(type);
                    newAggregate.setTargetId(targetId);
                    newAggregate.setTotalReviews(0);
                    newAggregate.setAvgRating(0.0);
                    return newAggregate;
                });

        Double newAvg = (ratingAggregate.getAvgRating() * ratingAggregate.getTotalReviews()
                + review.getOverallRating()) / (ratingAggregate.getTotalReviews() + 1);

        ratingAggregate.setTotalReviews(ratingAggregate.getTotalReviews() + 1);
        ratingAggregate.setAvgRating(newAvg);

        return ratingAggregate;
    }
}
