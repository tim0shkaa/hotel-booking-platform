package edu.hotel.notification.scheduler;

import edu.hotel.notification.entity.NotificationLog;
import edu.hotel.notification.model.NotificationStatus;
import edu.hotel.notification.repository.NotificationLogRepository;
import edu.hotel.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class NotificationExpirationScheduler {

    private final NotificationLogRepository notificationLogRepository;

    private final NotificationService notificationService;

    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void cancelExpiredNotification() {

        List<NotificationLog> failed = notificationLogRepository
                .findAllByStatusAndNextRetryAtLessThanEqual(NotificationStatus.FAILED, LocalDateTime.now());

        for (NotificationLog notificationLog : failed) {

            if (notificationLog.getRetryCount() >= 3) {
                continue;
            }

            try {
                notificationService.retrySendNotification(notificationLog);
                notificationLog.setStatus(NotificationStatus.SENT);
                notificationLog.setNextRetryAt(null);
            } catch (Exception e) {
                notificationLog.setStatus(NotificationStatus.FAILED);
                notificationLog.setNextRetryAt(LocalDateTime.now()
                        .plusMinutes((long) Math.pow(2, notificationLog.getRetryCount())));
            }

            notificationLogRepository.save(notificationLog);
        }
    }
}
