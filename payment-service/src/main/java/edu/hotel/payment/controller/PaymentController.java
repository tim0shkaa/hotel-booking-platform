package edu.hotel.payment.controller;

import edu.hotel.payment.dto.payment.PaymentResponse;
import edu.hotel.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<PaymentResponse> processPayment(
            @RequestBody Long bookingId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(paymentService.processPayment(bookingId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PaymentResponse> findPaymentByPaymentId(
            @PathVariable("id") Long id) {
        return ResponseEntity.ok(paymentService.findPaymentByPaymentId(id));
    }

    @GetMapping("/booking/{bookingId}")
    public ResponseEntity<PaymentResponse> findPaymentByBookingId(
            @PathVariable("bookingId") Long bookingId) {
        return ResponseEntity.ok(paymentService.findPaymentByBookingId(bookingId));
    }

    @GetMapping
    public ResponseEntity<Page<PaymentResponse>> getAllPayments(
            @RequestParam(required = false) Long guestId,
            Pageable pageable
    ) {
        return ResponseEntity.ok(paymentService.getAllPayments(guestId, pageable));
    }

    @PostMapping("/{id}/retry")
    public ResponseEntity<PaymentResponse> retryPayment(
            @PathVariable("id") Long id) {
        return ResponseEntity.ok(paymentService.retryPayment(id));
    }
}
