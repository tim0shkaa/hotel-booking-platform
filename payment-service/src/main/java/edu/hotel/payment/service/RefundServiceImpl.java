package edu.hotel.payment.service;

import edu.hotel.common.exception.NotFoundException;
import edu.hotel.payment.dto.refund.RefundResponse;
import edu.hotel.payment.entity.Payment;
import edu.hotel.payment.entity.Refund;
import edu.hotel.payment.exception.InvalidPaymentStatusException;
import edu.hotel.payment.exception.InvalidRefundAmountException;
import edu.hotel.payment.exception.InvalidRefundStatusException;
import edu.hotel.payment.mapper.RefundMapper;
import edu.hotel.payment.model.PaymentStatus;
import edu.hotel.payment.model.RefundStatus;
import edu.hotel.payment.provider.MockPaymentProvider;
import edu.hotel.payment.repository.PaymentRepository;
import edu.hotel.payment.repository.RefundRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class RefundServiceImpl implements RefundService {

    private final PaymentRepository paymentRepository;

    private final RefundRepository refundRepository;

    private final MockPaymentProvider mockPaymentProvider;

    private final RefundMapper refundMapper;

    @Override
    @Transactional
    public RefundResponse requestRefund(Long paymentId, BigDecimal amount, String reason) {

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new NotFoundException("Платежа с paymentId: " + paymentId + " не существует"));

        if (payment.getStatus() != PaymentStatus.CONFIRMED) {
            throw new InvalidPaymentStatusException("Статус платежа не CONFIRMED");
        }

        BigDecimal alreadyRefunded = refundRepository.findAllByPaymentId(paymentId)
                .stream()
                .map(Refund::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (amount.add(alreadyRefunded).compareTo(payment.getAmount()) > 0) {
            throw new InvalidRefundAmountException("Сумма возврата превышает доступную");
        }

        Refund refund = new Refund();
        refund.setPayment(payment);
        refund.setAmount(amount);
        refund.setReason(reason);
        refund.setStatus(RefundStatus.PENDING);

        refundRepository.save(refund);

        mockPaymentProvider.processRefund(refund.getId());

        return refundMapper.toResponse(refund);
    }

    @Override
    @Transactional
    public RefundResponse retryRefund(Long refundId) {

        Refund refund = refundRepository.findById(refundId)
                .orElseThrow(() -> new NotFoundException("Возврата с refundId: " + refundId + " не существует"));

        if (refund.getStatus() != RefundStatus.FAILED) {
            throw new InvalidRefundStatusException("Статус возврата не FAILED");
        }

        refund.setStatus(RefundStatus.PROCESSING);

        mockPaymentProvider.processRefund(refundId);

        return refundMapper.toResponse(refund);
    }

    @Override
    @Transactional(readOnly = true)
    public RefundResponse getRefundById(Long refundId) {
        Refund refund = refundRepository.findById(refundId)
                .orElseThrow(() -> new NotFoundException("Возврата с refundId: " + refundId + " не существует"));

        return refundMapper.toResponse(refund);
    }
}
