package edu.hotel.booking.service;

import edu.hotel.booking.entity.Booking;
import edu.hotel.booking.model.BookingStatus;

public interface BookingStatusHistoryService {

    void saveStatusHistory(Booking booking, BookingStatus status, String changedBy, String reason);
}
