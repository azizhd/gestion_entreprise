package com.example.backend.service;

import com.example.backend.audit.ActionType;
import com.example.backend.audit.AuditAction;
import com.example.backend.dto.NotificationDto;
import com.example.backend.entitie.enumuration.NotificationType;

import java.util.List;

public interface NotificationService {
    @AuditAction(action = "NOTIFICATION_SEND_USER", entityType = "Notification", type = ActionType.CREATE)
    void sendNotificationToUser(Long userId, String title, String message);

    @AuditAction(action = "NOTIFICATION_SEND_USER", entityType = "Notification", type = ActionType.CREATE)
    void sendNotificationToUser(Long userId, String title, String message, NotificationType type, Long relatedEntityId, String relatedEntityType);

    @AuditAction(action = "NOTIFICATION_SEND_USERS", entityType = "Notification", type = ActionType.CREATE)
    void sendNotificationToUsers(List<Long> userIds, String title, String message);

    @AuditAction(action = "NOTIFICATION_SEND_USERS_INAPP", entityType = "Notification", type = ActionType.CREATE)
    void sendInAppNotificationToUsers(List<Long> userIds, String title, String message);

    @AuditAction(action = "NOTIFICATION_SEND_USERS_INAPP", entityType = "Notification", type = ActionType.CREATE)
    void sendInAppNotificationToUsers(List<Long> userIds, String title, String message, NotificationType type, Long relatedEntityId, String relatedEntityType);

    @AuditAction(action = "EMAIL_SEND", entityType = "Email", type = ActionType.CREATE)
    void sendEmail(String to, String subject, String body);

    @AuditAction(action = "EMAIL_SEND_ATTACHMENT", entityType = "Email", type = ActionType.CREATE)
    void sendEmailWithAttachment(String to, String subject, String body, byte[] attachment, String filename);

    // New methods for controller
    List<NotificationDto> getNotificationsByUserId(Long userId, NotificationType type);

    NotificationDto getNotificationById(Long id);

    @AuditAction(action = "NOTIFICATION_MARK_READ", entityType = "Notification", type = ActionType.UPDATE)
    NotificationDto markAsRead(Long id);

    @AuditAction(action = "NOTIFICATION_DELETE", entityType = "Notification", type = ActionType.DELETE)
    void deleteNotification(Long id);
}
