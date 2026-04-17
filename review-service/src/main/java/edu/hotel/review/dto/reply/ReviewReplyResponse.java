package edu.hotel.review.dto.reply;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ReviewReplyResponse {

    private Long id;

    private Long reviewId;

    private Long authorId;

    private String body;

    private LocalDateTime createdAt;
}
