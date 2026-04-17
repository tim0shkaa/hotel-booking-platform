package edu.hotel.review.dto.review;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ReviewResponse {

    private Long id;

    private Long bookingId;

    private Long guestId;

    private Long hotelId;

    private Long roomTypeId;

    private Integer overallRating;

    private Integer cleanlinessRating;

    private Integer serviceRating;

    private Integer locationRating;

    private Integer valueRating;

    private String body;

    private Boolean isVerified;

    private LocalDateTime createdAt;
}
