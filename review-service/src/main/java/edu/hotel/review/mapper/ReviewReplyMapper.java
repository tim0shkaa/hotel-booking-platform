package edu.hotel.review.mapper;

import edu.hotel.review.dto.reply.ReviewReplyRequest;
import edu.hotel.review.dto.reply.ReviewReplyResponse;
import edu.hotel.review.dto.review.ReviewResponse;
import edu.hotel.review.entity.ReviewReply;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ReviewReplyMapper {

    ReviewReplyResponse toResponse(ReviewReply reviewReply);
}
