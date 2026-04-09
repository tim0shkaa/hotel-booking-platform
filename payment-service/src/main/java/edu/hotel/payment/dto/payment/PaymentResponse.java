package edu.hotel.payment.dto.payment;

import edu.hotel.payment.model.PaymentStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PaymentResponse {

    private Long id;

    private Long bookingId;

    private Long guestId;

    private BigDecimal amount;

    private String currency;

    private PaymentStatus status;

    private String provider;

    private String providerPaymentId;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
