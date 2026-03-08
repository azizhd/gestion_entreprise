package com.example.backend.repository;

import com.example.backend.entitie.Notification;
import com.example.backend.entitie.enumuration.NotificationStatus;
import com.example.backend.entitie.enumuration.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    @Query("select n from Notification n where n.utilisateur.id = :userId and (n.status <> com.example.backend.entitie.enumuration.NotificationStatus.DELETED or n.status is null) order by n.createdAt desc")
    List<Notification> findActiveByUser(@Param("userId") Long utilisateurId);

    @Query("select n from Notification n where n.utilisateur.id = :userId and n.type = :type and (n.status <> com.example.backend.entitie.enumuration.NotificationStatus.DELETED or n.status is null) order by n.createdAt desc")
    List<Notification> findActiveByUserAndType(@Param("userId") Long utilisateurId, @Param("type") NotificationType type);

    boolean existsByUtilisateurIdAndRelatedEntityIdAndTypeAndStatus(Long utilisateurId, Long relatedEntityId, NotificationType type, NotificationStatus status);

    boolean existsByUtilisateurIdAndRelatedEntityIdAndType(Long utilisateurId, Long relatedEntityId, NotificationType type);
}
