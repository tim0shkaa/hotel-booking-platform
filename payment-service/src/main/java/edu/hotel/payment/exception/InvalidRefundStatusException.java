package edu.hotel.payment.exception;

public class InvalidRefundStatusException extends RuntimeException {
    public InvalidRefundStatusException(String message) {
        super(message);
    }
}
