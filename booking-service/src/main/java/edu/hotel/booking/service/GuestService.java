package edu.hotel.booking.service;

import edu.hotel.booking.dto.booking.BookingSummaryResponse;
import edu.hotel.booking.dto.guest.GuestRequest;
import edu.hotel.booking.dto.guest.GuestResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface GuestService {

    // GET /guests/{guestId}/bookings
    Page<BookingSummaryResponse> getHistoryBookings(Long guestId, Long userId, String role, Pageable pageable);

    // POST /guests
    GuestResponse create(Long userId, GuestRequest request);

    // PUT /guests/me
    GuestResponse update(Long userId, GuestRequest request);
}
