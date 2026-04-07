package edu.hotel.auth.mapper;

import edu.hotel.auth.dto.audit.AuditLogResponse;
import edu.hotel.auth.entity.AuditLog;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AuditLogMapper {

    @Mapping(source = "user.id", target = "userId")
    AuditLogResponse toResponse(AuditLog auditLog);
}
