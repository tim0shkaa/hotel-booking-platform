package edu.hotel.booking.exception;

public class NotAvailableRoomsException extends RuntimeException {
    public NotAvailableRoomsException(String message) {
        super(message);
    }
}
