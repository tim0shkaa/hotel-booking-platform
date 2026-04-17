package edu.hotel.review.controller;

import edu.hotel.review.dto.reply.ReviewReplyRequest;
import edu.hotel.review.dto.reply.ReviewReplyResponse;
import edu.hotel.review.dto.review.ReviewRequest;
import edu.hotel.review.dto.review.ReviewResponse;
import edu.hotel.review.service.ReviewReplyService;
import edu.hotel.review.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    private final ReviewReplyService reviewReplyService;

    @PostMapping
    public ResponseEntity<ReviewResponse> create(
            @Valid @RequestBody ReviewRequest request,
            Authentication authentication
    ) {
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reviewService.createReview(request, userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReviewResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(reviewService.findById(id));
    }

    @PostMapping("/{id}/response")
    public ResponseEntity<ReviewReplyResponse> createReply(
            @PathVariable Long id,
            @Valid @RequestBody ReviewReplyRequest request,
            Authentication authentication
    ) {
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reviewReplyService.createReply(request, id, userId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        reviewService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
