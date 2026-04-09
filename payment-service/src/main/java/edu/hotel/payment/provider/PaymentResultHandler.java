package edu.hotel.payment.provider;

import edu.hotel.common.exception.NotFoundException;
import edu.hotel.payment.entity.Payment;
import edu.hotel.payment.entity.PaymentAttempt;
import edu.hotel.payment.entity.Refund;
import edu.hotel.payment.model.AttemptStatus;
import edu.hotel.payment.model.PaymentStatus;
import edu.hotel.payment.model.RefundStatus;
import edu.hotel.payment.repository.PaymentAttemptRepository;
import edu.hotel.payment.repository.PaymentRepository;
import edu.hotel.payment.repository.RefundRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class PaymentResultHandler {

    private final PaymentRepository paymentRepository;

    private final RefundRepository refundRepository;

    private final PaymentAttemptRepository paymentAttemptRepository;

    @Transactional
    public void handleProviderResult(Long paymentId, boolean success, String providerPaymentId, String errorCode, String errorMessage) {

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new NotFoundException("Платежа с paymentId: " + paymentId + " не существует"));

        payment.setStatus(success ? PaymentStatus.CONFIRMED : PaymentStatus.FAILED);
        payment.setProviderPaymentId(providerPaymentId);

        PaymentAttempt attempt = paymentAttemptRepository
                .findTopByPaymentIdOrderByAttemptNumberDesc(paymentId)
                .orElseThrow(() -> new NotFoundException("Попытка оплаты не найдена"));

        attempt.setStatus(success ? AttemptStatus.SUCCESS : AttemptStatus.FAILED);
        attempt.setErrorCode(errorCode);
        attempt.setErrorMessage(errorMessage);
        // TODO: KAFKA
    }

    @Transactional
    public void handleRefundResult(Long refundId, boolean success) {

        Refund refund = refundRepository.findById(refundId)
                .orElseThrow(() -> new NotFoundException("Возврата с refundId: " + refundId + " не существует"));

        refund.setStatus(success ? RefundStatus.PROCESSED : RefundStatus.FAILED);
        refund.setProcessedAt(LocalDateTime.now());

        // TODO: KAFKA
    }
}
