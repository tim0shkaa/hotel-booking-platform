package edu.hotel.review.service;

import edu.hotel.common.exception.AlreadyExistsException;
import edu.hotel.review.dto.reply.ReviewReplyRequest;
import edu.hotel.review.dto.reply.ReviewReplyResponse;
import edu.hotel.review.entity.ReviewReply;
import edu.hotel.review.mapper.ReviewReplyMapper;
import edu.hotel.review.repository.ReviewReplyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReviewReplyServiceImpl implements ReviewReplyService {

    private final ReviewReplyRepository reviewReplyRepository;

    private final ReviewReplyMapper reviewReplyMapper;

    @Override
    @Transactional
    public ReviewReplyResponse createReply(ReviewReplyRequest request, Long reviewId, Long userId) {

        if (reviewReplyRepository.existsByReviewId(reviewId)) {
            throw new AlreadyExistsException("Ответ на отзыв с id: " + reviewId + " уже существует");
        }

        ReviewReply reviewReply = new ReviewReply();
        reviewReply.setAuthorId(userId);
        reviewReply.setReviewId(reviewId);
        reviewReply.setBody(request.getBody());

        ReviewReply savedReply = reviewReplyRepository.save(reviewReply);

        return reviewReplyMapper.toResponse(savedReply);
    }
}
