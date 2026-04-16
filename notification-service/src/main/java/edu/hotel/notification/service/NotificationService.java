package edu.hotel.notification.service;

import edu.hotel.events.*;
import edu.hotel.notification.entity.NotificationLog;

public interface NotificationService {

    void handleBookingCreated(BookingCreatedEvent event);

    void handleBookingCancelled(BookingCancelledEvent event);

    void handleBookingCompleted(BookingCompletedEvent event);

    void handlePaymentConfirmed(PaymentConfirmedEvent event);

    void handlePaymentFailed(PaymentFailedEvent event);

    void retrySendNotification(NotificationLog notificationLog);
}
