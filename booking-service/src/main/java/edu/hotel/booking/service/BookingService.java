package edu.hotel.booking.service;

import edu.hotel.booking.dto.booking.BookingCreateRequest;
import edu.hotel.booking.dto.booking.BookingCreateResponse;
import edu.hotel.booking.dto.booking.BookingDetailResponse;
import edu.hotel.booking.dto.booking.BookingSummaryResponse;
import edu.hotel.booking.model.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface BookingService {

    // POST /bookings
    BookingCreateResponse create(BookingCreateRequest request, Long guestId);

    // GET /bookings/{id}
    BookingDetailResponse getById(Long id);

    // GET /bookings
    Page<BookingSummaryResponse> getBookingWithFilters(
            Long hotelId,
            BookingStatus status,
            LocalDate checkIn,
            LocalDate checkOut,
            Pageable pageable
    );

    // GET guests/{guestId}/bookings
    Page<BookingSummaryResponse> getHistoryBookings(Long guestId, Pageable pageable);

    // POST /bookings/{id}/cancel
    BookingDetailResponse cancelBooking(Long id);

    // POST bookings/{id}/check-in
    BookingDetailResponse checkInById(Long id);

    // POST bookings/{id}/check-out
    BookingDetailResponse checkOutById(Long id);

}
