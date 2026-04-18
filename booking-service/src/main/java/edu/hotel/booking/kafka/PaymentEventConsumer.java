package edu.hotel.booking.kafka;

import edu.hotel.booking.entity.Booking;
import edu.hotel.booking.entity.ProcessedEvent;
import edu.hotel.booking.model.BookingStatus;
import edu.hotel.booking.repository.BookingRepository;
import edu.hotel.booking.repository.ProcessedEventRepository;
import edu.hotel.booking.service.BookingStatusHistoryService;
import edu.hotel.common.model.KafkaTopics;
import edu.hotel.events.PaymentConfirmedEvent;
import edu.hotel.events.PaymentFailedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class PaymentEventConsumer {

    private final BookingRepository bookingRepository;

    private final ProcessedEventRepository processedEventRepository;

    private final BookingStatusHistoryService bookingStatusHistoryService;

    @KafkaListener(topics = KafkaTopics.PAYMENT_CONFIRMED, groupId = "booking-service-group")
    @Transactional
    public void handlePaymentConfirmed(PaymentConfirmedEvent event) {

        if (processedEventRepository.existsByEventId(event.getEventId())) {
            return;
        }

        Optional<Booking> bookingOpt = bookingRepository.findById(event.getBookingId());
        if (bookingOpt.isEmpty()) {
            return;
        }
        Booking booking = bookingOpt.get();

        booking.setStatus(BookingStatus.CONFIRMED);
        bookingRepository.save(booking);

        ProcessedEvent processedEvent = new ProcessedEvent();
        processedEvent.setEventId(event.getEventId());

        bookingStatusHistoryService.saveStatusHistory(booking, BookingStatus.CONFIRMED,
                "system", "Оплата прошла успешно");

        processedEventRepository.save(processedEvent);
    }

    @KafkaListener(topics = KafkaTopics.PAYMENT_FAILED, groupId = "booking-service-group")
    @Transactional
    public void handlePaymentFailed(PaymentFailedEvent event) {

        if (processedEventRepository.existsByEventId(event.getEventId())) {
            return;
        }

        Optional<Booking> bookingOpt = bookingRepository.findById(event.getBookingId());
        if (bookingOpt.isEmpty()) {
            return;
        }

        ProcessedEvent processedEvent = new ProcessedEvent();
        processedEvent.setEventId(event.getEventId());

        bookingStatusHistoryService.saveStatusHistory(bookingOpt.get(), BookingStatus.PENDING_PAYMENT,
                "system", "Попытка оплаты не удалась");

        processedEventRepository.save(processedEvent);
    }
}
