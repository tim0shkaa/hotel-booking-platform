package edu.hotel.payment.kafka;

import edu.hotel.common.model.KafkaTopics;
import edu.hotel.events.BookingCancelledEvent;
import edu.hotel.events.BookingCreatedEvent;
import edu.hotel.payment.entity.Payment;
import edu.hotel.payment.entity.ProcessedEvent;
import edu.hotel.payment.model.PaymentStatus;
import edu.hotel.payment.repository.PaymentRepository;
import edu.hotel.payment.repository.ProcessedEventRepository;
import edu.hotel.payment.service.PaymentService;
import edu.hotel.payment.service.RefundService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class BookingEventConsumer {

    private final ProcessedEventRepository processedEventRepository;

    private final PaymentService paymentService;

    private final PaymentRepository paymentRepository;

    private final RefundService refundService;

    @KafkaListener(topics = KafkaTopics.BOOKING_CREATED, groupId = "payment-service-group")
    public void handleBookingCreated(BookingCreatedEvent event) {

        if (processedEventRepository.existsByEventId(event.getEventId())) {
            return;
        }

        paymentService.initiatePayment(event.getBookingId(), event.getGuestId(),
                event.getTotalPrice(), event.getCurrency());

        ProcessedEvent processedEvent = new ProcessedEvent();
        processedEvent.setEventId(event.getEventId());

        processedEventRepository.save(processedEvent);
    }

    @KafkaListener(topics = KafkaTopics.BOOKING_CANCELLED, groupId = "payment-service-group")
    public void handleBookingCancelled(BookingCancelledEvent event) {

        if (processedEventRepository.existsByEventId(event.getEventId())) {
            return;
        }

        Optional<Payment> paymentOpt = paymentRepository.findByBookingId(event.getBookingId());
        if (paymentOpt.isEmpty()) {
            return;
        }
        Payment payment = paymentOpt.get();

        if (payment.getStatus() == PaymentStatus.CONFIRMED) {
            refundService.requestRefund(payment.getId(), payment.getAmount(), event.getReason());
        }

        ProcessedEvent processedEvent = new ProcessedEvent();
        processedEvent.setEventId(event.getEventId());

        processedEventRepository.save(processedEvent);
    }
}
