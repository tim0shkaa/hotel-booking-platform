package edu.hotel.payment.service;

import edu.hotel.payment.dto.refund.RefundResponse;

import java.math.BigDecimal;

public interface RefundService {

    // POST /payments/{id}/refund
    RefundResponse requestRefund(Long paymentId, BigDecimal amount, String reason);

    // POST /refunds/{id}/retry
    RefundResponse retryRefund(Long refundId, Long userId, String role);

    // GET /refunds/{id}
    RefundResponse getRefundById(Long refundId, Long userId, String role);
}
