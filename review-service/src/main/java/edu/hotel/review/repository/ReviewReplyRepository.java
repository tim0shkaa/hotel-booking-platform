package edu.hotel.review.repository;

import edu.hotel.review.entity.ReviewReply;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewReplyRepository extends JpaRepository<ReviewReply, Long> {

    boolean existsByReviewId(Long reviewId);
}
