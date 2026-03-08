package com.example.backend.dto;

import com.example.backend.entitie.enumuration.NotificationStatus;
import com.example.backend.entitie.enumuration.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDto {
    private Long id;
    private String titre;
    private String message;
    private LocalDateTime createdAt;
    private NotificationStatus status;
    private NotificationType type;
    private Long relatedEntityId;
    private String relatedEntityType;
    private Boolean lu;
    private Long utilisateurId;
}
