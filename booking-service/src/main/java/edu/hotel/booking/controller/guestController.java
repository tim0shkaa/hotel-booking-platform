package edu.hotel.booking.controller;

import edu.hotel.booking.dto.booking.BookingSummaryResponse;
import edu.hotel.booking.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/guests")
@RequiredArgsConstructor
public class guestController {

    private final BookingService bookingService;

    @GetMapping("/{guestId}/bookings")
    public ResponseEntity<Page<BookingSummaryResponse>> getHistoryBookings(
            @PathVariable Long guestId,
            Pageable pageable) {
        return ResponseEntity.ok(bookingService.getHistoryBookings(guestId, pageable));
    }
}
