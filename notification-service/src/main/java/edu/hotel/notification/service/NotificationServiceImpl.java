package edu.hotel.notification.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.hotel.events.*;
import edu.hotel.notification.entity.NotificationLog;
import edu.hotel.notification.model.EventType;
import edu.hotel.notification.model.NotificationStatus;
import edu.hotel.notification.repository.NotificationLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final ObjectMapper objectMapper;

    private final NotificationLogRepository notificationLogRepository;

    @Override
    @Transactional
    public void handleBookingCreated(BookingCreatedEvent event) {

        notificationLogRepository.save(setNotificationLog(
                EventType.BOOKING_CREATED, event.getGuestId(), event.getBookingId(), event));
    }

    @Override
    @Transactional
    public void handleBookingCancelled(BookingCancelledEvent event) {

        notificationLogRepository.save(setNotificationLog(
                EventType.BOOKING_CANCELLED, event.getGuestId(), event.getBookingId(), event));
    }

    @Override
    @Transactional
    public void handleBookingCompleted(BookingCompletedEvent event) {

        notificationLogRepository.save(setNotificationLog(
                EventType.BOOKING_COMPLETED, event.getGuestId(), event.getBookingId(), event));
    }

    @Override
    @Transactional
    public void handlePaymentConfirmed(PaymentConfirmedEvent event) {
        notificationLogRepository.save(setNotificationLog(
                EventType.PAYMENT_CONFIRMED, event.getGuestId(), event.getBookingId(), event));
    }

    @Override
    @Transactional
    public void handlePaymentFailed(PaymentFailedEvent event) {

        notificationLogRepository.save(setNotificationLog(
                EventType.PAYMENT_FAILED, event.getGuestId(), event.getBookingId(), event));
    }

    private NotificationLog setNotificationLog(
            EventType eventType, Long guestId, Long bookingId, Object event) {

        NotificationLog notificationLog = new NotificationLog();
        notificationLog.setEventType(eventType);
        notificationLog.setGuestId(guestId);
        notificationLog.setBookingId(bookingId);
        notificationLog.setRetryCount(0);

        try {
            notificationLog.setPayload(objectMapper.writeValueAsString(event));
            sendNotification(eventType, guestId);
            notificationLog.setStatus(NotificationStatus.SENT);
        } catch (JsonProcessingException e) {
            notificationLog.setPayload("serialization error");
        } catch (Exception e) {
            notificationLog.setStatus(NotificationStatus.FAILED);
            notificationLog.setNextRetryAt(LocalDateTime.now().plusMinutes(1));
        }

        return notificationLog;
    }

    private void sendNotification(EventType eventType, Long guestId) {
        log.info("Отправка уведомления [{}] гостю с id: {}", eventType, guestId);
    }

    public void retrySendNotification(NotificationLog notificationLog) {
        log.info("Повторная отправка уведомления [{}] гостю с id: {}",
                notificationLog.getEventType(), notificationLog.getGuestId());
    }
}
