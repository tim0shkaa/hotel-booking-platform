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
        WHERE (CAST(:userId AS BIGINT) IS NULL OR user_id = CAST(:userId AS BIGINT))
        AND (CAST(:action AS VARCHAR) IS NULL OR action = CAST(:action AS VARCHAR))
        AND (CAST(:from AS TIMESTAMP) IS NULL OR created_at >= CAST(:from AS TIMESTAMP))
        AND (CAST(:to AS TIMESTAMP) IS NULL OR created_at <= CAST(:to AS TIMESTAMP))
        """,
            countQuery = """
        SELECT COUNT(*) FROM audit_logs
        WHERE (CAST(:userId AS BIGINT) IS NULL OR user_id = CAST(:userId AS BIGINT))
        AND (CAST(:action AS VARCHAR) IS NULL OR action = CAST(:action AS VARCHAR))
        AND (CAST(:from AS TIMESTAMP) IS NULL OR created_at >= CAST(:from AS TIMESTAMP))
        AND (CAST(:to AS TIMESTAMP) IS NULL OR created_at <= CAST(:to AS TIMESTAMP))
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
