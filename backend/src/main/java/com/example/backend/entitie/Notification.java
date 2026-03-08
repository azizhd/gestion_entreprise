package com.example.backend.entitie;

import com.example.backend.entitie.enumuration.NotificationStatus;
import com.example.backend.entitie.enumuration.NotificationType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "notification")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor


public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String titre;
    private String message;

    @Column(name = "date_envoi")
    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    private NotificationStatus status;

    @Enumerated(EnumType.STRING)
    private NotificationType type;

    private Long relatedEntityId;

    private String relatedEntityType;

    private Boolean lu;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private Utilisateur utilisateur;

}

