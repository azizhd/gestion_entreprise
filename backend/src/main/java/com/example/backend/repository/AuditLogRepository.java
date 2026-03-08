package com.example.backend.repository;

import com.example.backend.audit.ActionType;
import com.example.backend.entitie.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    @Query("SELECT a FROM AuditLog a " +
            "WHERE (:action IS NULL OR a.action = :action) " +
            "AND (:entityType IS NULL OR a.entityType = :entityType) " +
            "AND (:userId IS NULL OR a.utilisateur.id = :userId) " +
            "AND (:actionType IS NULL OR a.actionType = :actionType) " +
            "AND (:fromDate IS NULL OR a.date >= :fromDate) " +
            "AND (:toDate IS NULL OR a.date <= :toDate)")
    Page<AuditLog> search(@Param("action") String action,
                          @Param("entityType") String entityType,
                          @Param("userId") Long userId,
                          @Param("actionType") ActionType actionType,
                          @Param("fromDate") LocalDateTime fromDate,
                          @Param("toDate") LocalDateTime toDate,
                          Pageable pageable);

    @Query("SELECT a FROM AuditLog a WHERE a.utilisateur.email = :email " +
            "AND (:actionType IS NULL OR a.actionType = :actionType) " +
            "AND (:fromDate IS NULL OR a.date >= :fromDate) " +
            "AND (:toDate IS NULL OR a.date <= :toDate)")
    Page<AuditLog> findByUserEmail(@Param("email") String email,
                                   @Param("actionType") ActionType actionType,
                                   @Param("fromDate") LocalDateTime fromDate,
                                   @Param("toDate") LocalDateTime toDate,
                                   Pageable pageable);
}
