package edu.hotel.booking.scheduler;

import edu.hotel.booking.entity.Booking;
import edu.hotel.booking.kafka.BookingEventProducer;
import edu.hotel.booking.model.BookingStatus;
import edu.hotel.booking.repository.BookingRepository;
import edu.hotel.booking.service.BookingStatusHistoryService;
import edu.hotel.common.model.KafkaTopics;
import edu.hotel.events.BookingCancelledEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class BookingExpirationScheduler {

    private final BookingRepository bookingRepository;

    private final BookingEventProducer bookingEventProducer;

    private final BookingStatusHistoryService bookingStatusHistoryService;

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void cancelExpiredBooking() {
        LocalDateTime expirationTime = LocalDateTime.now().minusMinutes(15);

        List<Booking> expiredBookings = bookingRepository
                .findAllByStatusAndCreatedAtBefore(BookingStatus.PENDING_PAYMENT, expirationTime);

        for (Booking booking : expiredBookings) {
            booking.setStatus(BookingStatus.CANCELLED);

            bookingStatusHistoryService.saveStatusHistory(booking, BookingStatus.CANCELLED,
                    "system", "Истек срок оплаты");

            bookingEventProducer.sendBookingCancelled(
                    BookingCancelledEvent.builder()
                            .eventId(UUID.randomUUID().toString())
                            .eventType(KafkaTopics.BOOKING_CANCELLED)
                            .bookingId(booking.getId())
                            .guestId(booking.getGuest().getId())
                            .reason("Истек срок оплаты")
                            .occurredAt(LocalDateTime.now())
                            .build()
            );
        }
    }
}
