package edu.hotel.payment.provider;

import edu.hotel.common.exception.NotFoundException;
import edu.hotel.common.model.KafkaTopics;
import edu.hotel.events.PaymentConfirmedEvent;
import edu.hotel.events.PaymentFailedEvent;
import edu.hotel.payment.entity.Payment;
import edu.hotel.payment.entity.PaymentAttempt;
import edu.hotel.payment.entity.Refund;
import edu.hotel.payment.kafka.PaymentEventProducer;
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
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PaymentResultHandler {

    private final PaymentRepository paymentRepository;

    private final RefundRepository refundRepository;

    private final PaymentAttemptRepository paymentAttemptRepository;

    private final PaymentEventProducer paymentEventProducer;

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

        if (success) {
            PaymentConfirmedEvent paymentConfirmedEvent = PaymentConfirmedEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .eventType(KafkaTopics.PAYMENT_CONFIRMED)
                    .paymentId(paymentId)
                    .bookingId(payment.getBookingId())
                    .guestId(payment.getGuestId())
                    .amount(payment.getAmount())
                    .currency(payment.getCurrency())
                    .occurredAt(LocalDateTime.now())
                    .build();

            paymentEventProducer.sendPaymentConfirmed(paymentConfirmedEvent);
        } else {
            PaymentFailedEvent paymentFailedEvent = PaymentFailedEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .eventType(KafkaTopics.PAYMENT_FAILED)
                    .paymentId(paymentId)
                    .bookingId(payment.getBookingId())
                    .guestId(payment.getGuestId())
                    .reason(errorMessage)
                    .occurredAt(LocalDateTime.now())
                    .build();

            paymentEventProducer.sendPaymentFailed(paymentFailedEvent);
        }
    }

    @Transactional
    public void handleRefundResult(Long refundId, boolean success) {

        Refund refund = refundRepository.findById(refundId)
                .orElseThrow(() -> new NotFoundException("Возврата с refundId: " + refundId + " не существует"));

        refund.setStatus(success ? RefundStatus.PROCESSED : RefundStatus.FAILED);
        if (success) {
            refund.setProcessedAt(LocalDateTime.now());
        }
    }
}
