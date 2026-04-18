package edu.hotel.payment.service;

import edu.hotel.payment.dto.payment.PaymentRequest;
import edu.hotel.payment.dto.payment.PaymentResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;

public interface PaymentService {

    // Вызывается из KafkaConsumer при получении booking.created
    void initiatePayment(Long bookingId, Long guestId, Long userId, BigDecimal amount, String currency);

    // POST /payments
    PaymentResponse processPayment(PaymentRequest request);

    // POST /payments/{id}/retry
    PaymentResponse retryPayment(Long paymentId, Long userId, String role);

    // GET /payments/{id}
    PaymentResponse findPaymentByPaymentId(Long paymentId, Long userId, String role);

    // GET /payments/booking/{bookingId}
    PaymentResponse findPaymentByBookingId(Long bookingId, Long userId, String role);

    // GET /payments
    Page<PaymentResponse> getAllPayments(Long guestId, Pageable pageable);
}
