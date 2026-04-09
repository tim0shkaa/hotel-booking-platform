package edu.hotel.payment.provider;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class MockPaymentProvider {

    private final PaymentResultHandler paymentResultHandler;

    @Value("${provider.mock.delay-ms}")
    private Long delayMs;

    @Value("${payment.mock.success-probability}")
    private double successProbability;

    @Async
    public void processPayment(Long paymentId, Long attemptId) {

        long actualDelay = delayMs + (long) (2 * Math.random() * delayMs);

        try {
            Thread.sleep(actualDelay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        boolean success = Math.random() < successProbability;

        if (success) {
            String providerPaymentId = "MOCK-" + UUID.randomUUID();
            paymentResultHandler.handleProviderResult(
                    paymentId, true, providerPaymentId, null, null
            );
        } else {
            paymentResultHandler.handleProviderResult(
                    paymentId, false, null, "PAYMENT_FAILED", "Отказ mock-провайдера"
            );
        }
    }

    @Async
    public void processRefund(Long refundId) {

        long actualDelay = delayMs + (long) (2 * Math.random() * delayMs);

        try {
            Thread.sleep(actualDelay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        boolean success = Math.random() < successProbability;

        paymentResultHandler.handleRefundResult(refundId, success);
    }
}
