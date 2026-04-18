package edu.hotel.payment.service;

import edu.hotel.common.exception.AccessDeniedException;
import edu.hotel.common.exception.NotFoundException;
import edu.hotel.payment.dto.payment.PaymentRequest;
import edu.hotel.payment.dto.payment.PaymentResponse;
import edu.hotel.payment.entity.Payment;
import edu.hotel.payment.entity.PaymentAttempt;
import edu.hotel.payment.exception.InvalidPaymentStatusException;
import edu.hotel.payment.mapper.PaymentMapper;
import edu.hotel.payment.model.AttemptStatus;
import edu.hotel.payment.model.PaymentStatus;
import edu.hotel.payment.provider.MockPaymentProvider;
import edu.hotel.payment.repository.PaymentAttemptRepository;
import edu.hotel.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    @Value("${payment.provider.name}")
    private String mockProvider;

    private final PaymentRepository paymentRepository;
    private final MockPaymentProvider mockPaymentProvider;
    private final PaymentAttemptRepository paymentAttemptRepository;
    private final PaymentMapper paymentMapper;

    @Override
    @Transactional
    public void initiatePayment(Long bookingId, Long guestId, Long userId, BigDecimal amount, String currency) {
        if (paymentRepository.findByBookingId(bookingId).isPresent()) {
            return;
        }

        Payment payment = new Payment();
        payment.setBookingId(bookingId);
        payment.setGuestId(guestId);
        payment.setUserId(userId);
        payment.setAmount(amount);
        payment.setCurrency(currency);
        payment.setStatus(PaymentStatus.PENDING);
        payment.setProvider(mockProvider);

        paymentRepository.save(payment);
    }

    @Override
    @Transactional
    public PaymentResponse processPayment(PaymentRequest request) {
        Long bookingId = request.getBookingId();

        Payment payment = paymentRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new NotFoundException("Платежа с bookingId: " + bookingId + " не существует"));

        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new InvalidPaymentStatusException("Статус платежа не PENDING");
        }

        payment.setStatus(PaymentStatus.PROCESSING);

        PaymentAttempt attempt = new PaymentAttempt();
        attempt.setAttemptNumber(1);
        attempt.setPayment(payment);
        attempt.setStatus(AttemptStatus.IN_PROGRESS);
        paymentAttemptRepository.save(attempt);

        mockPaymentProvider.processPayment(payment.getId(), attempt.getId());

        return paymentMapper.toResponse(payment);
    }

    @Override
    @Transactional
    public PaymentResponse retryPayment(Long paymentId, Long userId, String role) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new NotFoundException("Платежа с paymentId: " + paymentId + " не существует"));

        if (role.equals("ROLE_GUEST") && !payment.getUserId().equals(userId)) {
            throw new AccessDeniedException("Нет доступа к чужому платежу");
        }

        if (payment.getStatus() != PaymentStatus.FAILED) {
            throw new InvalidPaymentStatusException("Статус платежа не FAILED");
        }

        payment.setStatus(PaymentStatus.PROCESSING);

        int nextAttemptNumber = paymentAttemptRepository
                .findTopByPaymentIdOrderByAttemptNumberDesc(paymentId)
                .map(a -> a.getAttemptNumber() + 1)
                .orElse(1);

        PaymentAttempt attempt = new PaymentAttempt();
        attempt.setAttemptNumber(nextAttemptNumber);
        attempt.setPayment(payment);
        paymentAttemptRepository.save(attempt);

        mockPaymentProvider.processPayment(payment.getId(), attempt.getId());

        return paymentMapper.toResponse(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse findPaymentByPaymentId(Long paymentId, Long userId, String role) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new NotFoundException("Платежа с paymentId: " + paymentId + " не существует"));

        if (role.equals("ROLE_GUEST") && !payment.getUserId().equals(userId)) {
            throw new AccessDeniedException("Нет доступа к чужому платежу");
        }

        return paymentMapper.toResponse(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse findPaymentByBookingId(Long bookingId, Long userId, String role) {
        Payment payment = paymentRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new NotFoundException("Платежа с bookingId: " + bookingId + " не существует"));

        if (role.equals("ROLE_GUEST") && !payment.getUserId().equals(userId)) {
            throw new AccessDeniedException("Нет доступа к чужому платежу");
        }

        return paymentMapper.toResponse(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PaymentResponse> getAllPayments(Long guestId, Pageable pageable) {
        if (guestId != null) {
            return paymentRepository.findAllByGuestId(guestId, pageable)
                    .map(paymentMapper::toResponse);
        }
        return paymentRepository.findAll(pageable)
                .map(paymentMapper::toResponse);
    }
}