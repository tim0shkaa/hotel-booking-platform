package edu.hotel.payment.service;

import edu.hotel.payment.dto.payment.PaymentRequest;
import edu.hotel.payment.dto.payment.PaymentResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;

public interface PaymentService {

    // Вызывается из KafkaConsumer при получении booking.created
    void initiatePayment(Long bookingId, Long guestId, BigDecimal amount, String currency);

    // POST /payments
    PaymentResponse processPayment(PaymentRequest request);

    // POST /payments/{id}/retry
    PaymentResponse retryPayment(Long paymentId);

    // GET /payments/{id}
    PaymentResponse findPaymentByPaymentId(Long paymentId);

    // GET /payments/booking/{bookingId}
    PaymentResponse findPaymentByBookingId(Long bookingId);

    // GET /payments
    Page<PaymentResponse> getAllPayments(Long guestId, Pageable pageable);
}
