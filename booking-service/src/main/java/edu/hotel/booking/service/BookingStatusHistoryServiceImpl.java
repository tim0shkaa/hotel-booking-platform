package edu.hotel.booking.service;

import edu.hotel.booking.entity.Booking;
import edu.hotel.booking.entity.BookingStatusHistory;
import edu.hotel.booking.model.BookingStatus;
import edu.hotel.booking.repository.BookingStatusHistoryRepository;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class BookingStatusHistoryServiceImpl implements BookingStatusHistoryService {

    private final BookingStatusHistoryRepository bookingStatusHistoryRepository;

    @Override
    public void saveStatusHistory(Booking booking, BookingStatus status, String changedBy, String reason) {

        BookingStatusHistory statusHistory = new BookingStatusHistory();
        statusHistory.setBooking(booking);
        statusHistory.setChangedBy(changedBy);
        statusHistory.setStatus(status);
        statusHistory.setReason(reason);

        bookingStatusHistoryRepository.save(statusHistory);
    }
}
