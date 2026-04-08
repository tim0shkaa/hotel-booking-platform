package edu.hotel.payment.dto.refund;

import edu.hotel.payment.entity.Payment;
import edu.hotel.payment.model.RefundStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class RefundResponse {

    private Long id;

    private Payment payment;

    private BigDecimal amount;

    private String reason;

    private RefundStatus status;

    private LocalDateTime createdAt;

    private LocalDateTime processedAt;
}
