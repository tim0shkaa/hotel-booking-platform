package edu.hotel.auth.dto.audit;

import edu.hotel.auth.model.Action;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AuditLogResponse {

    private Long id;

    private Long userId;

    private Action action;

    private String ipAddress;

    private String userAgent;

    private LocalDateTime createdAt;
}
