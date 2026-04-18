package edu.hotel.booking.controller;

import edu.hotel.booking.dto.booking.BookingSummaryResponse;
import edu.hotel.booking.dto.guest.GuestRequest;
import edu.hotel.booking.dto.guest.GuestResponse;
import edu.hotel.booking.service.BookingService;
import edu.hotel.booking.service.GuestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/guests")
@RequiredArgsConstructor
public class GuestController {

    private final GuestService guestService;

    @GetMapping("/{guestId}/bookings")
    public ResponseEntity<Page<BookingSummaryResponse>> getHistoryBookings(
            @PathVariable Long guestId,
            Authentication authentication,
            Pageable pageable) {
        Long userId = (Long) authentication.getPrincipal();
        String role = authentication.getAuthorities().iterator().next().getAuthority();
        return ResponseEntity.ok(guestService.getHistoryBookings(guestId, userId, role, pageable));
    }

    @PostMapping
    public ResponseEntity<GuestResponse> create(
            Authentication authentication,
            @Valid @RequestBody GuestRequest request
    ) {
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED).body(guestService.create(userId, request));
    }

    @PutMapping("/me")
    public ResponseEntity<GuestResponse> update(
            Authentication authentication,
            @Valid @RequestBody GuestRequest request
    ) {
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(guestService.update(userId, request));
    }
}
