package com.example.backend.dto;

import com.example.backend.audit.ActionType;

import java.time.LocalDateTime;

public record AuditLogSearchRequest(
        String action,
        String entityType,
        Long userId,
        ActionType actionType,
        LocalDateTime from,
        LocalDateTime to
) {}
