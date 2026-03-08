package com.example.backend.service;

import com.example.backend.audit.ActionType;
import com.example.backend.dto.AuditLogDto;
import com.example.backend.dto.AuditLogSearchRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;

public interface AuditLogService {
    void record(AuditLogRecord record);

    Page<AuditLogDto> search(AuditLogSearchRequest request, Pageable pageable);

    Page<AuditLogDto> searchCurrentUser(ActionType actionType, LocalDateTime from, LocalDateTime to, Pageable pageable);

    record AuditLogRecord(String action,
                          ActionType type,
                          String contenu,
                          String entityType,
                          Long entityId,
                          Boolean success,
                          String errorMessage,
                          String principalEmail) {}
}
