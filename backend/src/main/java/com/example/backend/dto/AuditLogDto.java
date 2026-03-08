package com.example.backend.dto;

import com.example.backend.audit.ActionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogDto {
    private Long id;
    private String action;
    private ActionType actionType;
    private String contenu;
    private LocalDateTime date;
    private String entityType;
    private Long entityId;
    private Boolean success;
    private String errorMessage;
    private Long utilisateurId;
    private String utilisateurEmail;
}
