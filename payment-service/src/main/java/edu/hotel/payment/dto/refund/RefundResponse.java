package edu.hotel.payment.dto.refund;

import edu.hotel.payment.entity.Payment;
import edu.hotel.payment.model.RefundStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class RefundResponse {

    private Long id;

    private Long paymentId;

    private BigDecimal amount;

    private String reason;

    private RefundStatus status;

    private LocalDateTime createdAt;

    private LocalDateTime processedAt;
}
