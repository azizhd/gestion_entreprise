package com.example.backend.controller;

import com.example.backend.dto.NotificationDto;
import com.example.backend.service.NotificationService;
import com.example.backend.entitie.enumuration.NotificationType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    public static class CreateNotificationRequest {
        public String titre;
        public String message;
    }

    public static class SendToUsersRequest {
        public List<Long> userIds;
        public String titre;
        public String message;
    }

    public static class EmailRequest {
        public String to;
        public String subject;
        public String body;
    }

    @PostMapping("/user/{userId}")
    public ResponseEntity<Void> sendToUser(@PathVariable Long userId, @RequestBody CreateNotificationRequest req) {
        notificationService.sendNotificationToUser(userId, req.titre, req.message);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/users")
    public ResponseEntity<Void> sendToUsers(@RequestBody SendToUsersRequest req) {
        notificationService.sendNotificationToUsers(req.userIds, req.titre, req.message);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/email")
    public ResponseEntity<Void> sendEmail(@RequestBody EmailRequest req) {
        notificationService.sendEmail(req.to, req.subject, req.body);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<NotificationDto>> getByUser(@PathVariable Long userId,
                                                           @RequestParam(value = "type", required = false) NotificationType type) {
        return ResponseEntity.ok(notificationService.getNotificationsByUserId(userId, type));
    }

    @GetMapping("/{id}")
    public ResponseEntity<NotificationDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(notificationService.getNotificationById(id));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<NotificationDto> markAsRead(@PathVariable Long id) {
        return ResponseEntity.ok(notificationService.markAsRead(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        notificationService.deleteNotification(id);
        return ResponseEntity.noContent().build();
    }
}
