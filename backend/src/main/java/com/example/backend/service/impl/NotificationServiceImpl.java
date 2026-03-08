package com.example.backend.service.impl;

import com.example.backend.dto.NotificationDto;
import com.example.backend.entitie.Notification;
import com.example.backend.entitie.Utilisateur;
import com.example.backend.entitie.enumuration.NotificationStatus;
import com.example.backend.entitie.enumuration.NotificationType;
import com.example.backend.mapper.NotificationMapper;
import com.example.backend.repository.NotificationRepository;
import com.example.backend.repository.UtilisateurRepository;
import com.example.backend.service.NotificationService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final JavaMailSender mailSender;
    private final NotificationMapper notificationMapper;

    public NotificationServiceImpl(NotificationRepository notificationRepository,
                                   UtilisateurRepository utilisateurRepository,
                                   JavaMailSender mailSender,
                                   NotificationMapper notificationMapper) {
        this.notificationRepository = notificationRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.mailSender = mailSender;
        this.notificationMapper = notificationMapper;
    }

    @Override
    public void sendEmail(String to, String subject, String body) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, true);
            mailSender.send(mimeMessage);
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send email", e);
        }
    }

    @Override
    public void sendEmailWithAttachment(String to, String subject, String body, byte[] attachment, String filename) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, true);
            if (attachment != null && filename != null) {
                helper.addAttachment(filename, new ByteArrayResource(attachment));
            }
            mailSender.send(mimeMessage);
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send email with attachment", e);
        }
    }

    @Override
    public void sendNotificationToUser(Long userId, String title, String message) {
        sendNotificationToUser(userId, title, message, NotificationType.GENERAL, null, null);
    }

    @Override
    public void sendNotificationToUser(Long userId, String title, String message, NotificationType type, Long relatedEntityId, String relatedEntityType) {
        Utilisateur user = utilisateurRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Notification notification = buildNotification(user, title, message, type, relatedEntityId, relatedEntityType);
        if (shouldPersist(user.getId(), notification)) {
            notificationRepository.save(notification);
        }
        if (user.getEmail() != null) {
            sendEmail(user.getEmail(), title, message);
        }
    }

@Override
    public void sendNotificationToUsers(List<Long> userIds, String title, String message) {
        for (Long userId : userIds) {
            sendNotificationToUser(userId, title, message, NotificationType.GENERAL, null, null);
        }
    }

    @Override
    public void sendInAppNotificationToUsers(List<Long> userIds, String title, String message) {
        sendInAppNotificationToUsers(userIds, title, message, NotificationType.GENERAL, null, null);
    }

    @Override
    public void sendInAppNotificationToUsers(List<Long> userIds, String title, String message, NotificationType type, Long relatedEntityId, String relatedEntityType) {
        NotificationType safeType = type != null ? type : NotificationType.GENERAL;
        for (Long userId : userIds) {
            Utilisateur user = utilisateurRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            Notification notification = buildNotification(user, title, message, safeType, relatedEntityId, relatedEntityType);
            if (shouldPersist(userId, notification)) {
                notificationRepository.save(notification);
            }
        }
    }


    @Override
    public List<NotificationDto> getNotificationsByUserId(Long userId, NotificationType type) {
        List<Notification> notifications = type == null
            ? notificationRepository.findActiveByUser(userId)
            : notificationRepository.findActiveByUserAndType(userId, type);
        return notifications.stream()
                .map(notificationMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public NotificationDto getNotificationById(Long id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Notification not found"));
        return notificationMapper.toDto(notification);
    }

    @Override
    public NotificationDto markAsRead(Long id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Notification not found"));
        notification.setLu(true);
        notification.setStatus(NotificationStatus.READ);
        notificationRepository.save(notification);
        return notificationMapper.toDto(notification);
    }

    @Override
    public void deleteNotification(Long id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Notification not found"));
        notification.setStatus(NotificationStatus.DELETED);
        notificationRepository.save(notification);
    }

    private Notification buildNotification(Utilisateur user, String title, String message, NotificationType type, Long relatedEntityId, String relatedEntityType) {
        Notification notification = new Notification();
        notification.setTitre(title);
        notification.setMessage(message);
        notification.setCreatedAt(LocalDateTime.now());
        notification.setStatus(NotificationStatus.UNREAD);
        notification.setLu(false);
        notification.setType(type != null ? type : NotificationType.GENERAL);
        notification.setRelatedEntityId(relatedEntityId);
        notification.setRelatedEntityType(relatedEntityType);
        notification.setUtilisateur(user);
        return notification;
    }

    private boolean shouldPersist(Long userId, Notification candidate) {
        if (candidate.getRelatedEntityId() == null || candidate.getType() == null) {
            return true;
        }
        // Do not recreate if any notification already existed for that entity/type, regardless of status
        return !notificationRepository.existsByUtilisateurIdAndRelatedEntityIdAndType(
                userId,
                candidate.getRelatedEntityId(),
                candidate.getType()
        );
    }

}
