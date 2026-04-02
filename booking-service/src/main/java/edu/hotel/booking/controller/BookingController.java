package edu.hotel.booking.controller;

import edu.hotel.booking.dto.booking.BookingCreateRequest;
import edu.hotel.booking.dto.booking.BookingCreateResponse;
import edu.hotel.booking.dto.booking.BookingDetailResponse;
import edu.hotel.booking.dto.booking.BookingSummaryResponse;
import edu.hotel.booking.model.BookingStatus;
import edu.hotel.booking.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    public ResponseEntity<BookingCreateResponse> create(
            @Valid @RequestBody BookingCreateRequest request,
            @RequestParam Long guestId) {
        BookingCreateResponse response = bookingService.create(request, guestId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<BookingDetailResponse> getById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(bookingService.getById(id));
    }

    @GetMapping
    public ResponseEntity<Page<BookingSummaryResponse>> getBookingWithFilters(
            @RequestParam(required = false) Long hotelId,
            @RequestParam(required = false) BookingStatus status,
            @RequestParam(required = false) LocalDate checkIn,
            @RequestParam(required = false) LocalDate checkOut,
            Pageable pageable
    ) {
        Page<BookingSummaryResponse> response = bookingService.getBookingWithFilters(
                hotelId, status, checkIn, checkOut, pageable);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<BookingDetailResponse> cancelBooking(@PathVariable("id") Long id) {
        BookingDetailResponse response = bookingService.cancelBooking(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/check-in")
    public ResponseEntity<BookingDetailResponse> checkInById(@PathVariable("id") Long id) {
        BookingDetailResponse response = bookingService.checkInById(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/check-out")
    public ResponseEntity<BookingDetailResponse> checkOutById(@PathVariable("id") Long id) {
        BookingDetailResponse response = bookingService.checkOutById(id);
        return ResponseEntity.ok(response);
    }
}
