package edu.hotel.notification.repository;

import edu.hotel.notification.entity.NotificationLog;
import edu.hotel.notification.model.NotificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long> {
    List<NotificationLog> findAllByStatusAndNextRetryAtLessThanEqual(
            NotificationStatus status, LocalDateTime now);
}
