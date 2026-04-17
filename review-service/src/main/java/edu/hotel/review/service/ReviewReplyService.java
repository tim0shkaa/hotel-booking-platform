package edu.hotel.review.service;

import edu.hotel.review.dto.reply.ReviewReplyRequest;
import edu.hotel.review.dto.reply.ReviewReplyResponse;

public interface ReviewReplyService {

    // POST /reviews/{id}/response
    ReviewReplyResponse createReply(ReviewReplyRequest request, Long reviewId, Long userId);
}
