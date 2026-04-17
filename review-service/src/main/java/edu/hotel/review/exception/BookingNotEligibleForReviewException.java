package edu.hotel.review.exception;

public class BookingNotEligibleForReviewException extends RuntimeException {
    public BookingNotEligibleForReviewException(String message) {
        super(message);
    }
}
