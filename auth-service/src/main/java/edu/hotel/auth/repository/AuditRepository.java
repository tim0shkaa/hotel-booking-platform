package edu.hotel.auth.repository;

import edu.hotel.auth.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface AuditRepository extends JpaRepository<AuditLog, Long> {
    @Query(value = """
        SELECT * FROM audit_logs
        WHERE (:userId IS NULL OR user_id = :userId)
        AND (:action IS NULL OR action = :action)
        AND (:from IS NULL OR created_at >= :from)
        AND (:to IS NULL OR created_at <= :to)
        """,
            countQuery = """
        SELECT COUNT(*) FROM audit_logs
        WHERE (:userId IS NULL OR user_id = :userId)
        AND (:action IS NULL OR action = :action)
        AND (:from IS NULL OR created_at >= :from)
        AND (:to IS NULL OR created_at <= :to)
        """,
            nativeQuery = true)
    Page<AuditLog> findAllWithFilters(
            @Param("userId") Long userId,
            @Param("action") String action,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable
    );
}
