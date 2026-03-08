package com.example.backend.service.impl;

import com.example.backend.audit.ActionType;
import com.example.backend.dto.AuditLogDto;
import com.example.backend.dto.AuditLogSearchRequest;
import com.example.backend.entitie.AuditLog;
import com.example.backend.entitie.Utilisateur;
import com.example.backend.mapper.AuditLogMapper;
import com.example.backend.repository.AuditLogRepository;
import com.example.backend.repository.UtilisateurRepository;
import com.example.backend.service.AuditLogService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Transactional
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final AuditLogMapper auditLogMapper;

    public AuditLogServiceImpl(AuditLogRepository auditLogRepository,
                               UtilisateurRepository utilisateurRepository,
                               AuditLogMapper auditLogMapper) {
        this.auditLogRepository = auditLogRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.auditLogMapper = auditLogMapper;
    }

    @Override
    public void record(AuditLogRecord record) {
        AuditLog log = new AuditLog();
        log.setAction(record.action());
        log.setActionType(record.type());
        log.setContenu(record.contenu());
        log.setEntityType(record.entityType());
        log.setEntityId(record.entityId());
        log.setSuccess(record.success());
        log.setErrorMessage(record.errorMessage());
        log.setDate(LocalDateTime.now());

        resolveUser(record.principalEmail()).ifPresent(log::setUtilisateur);

        auditLogRepository.save(log);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLogDto> search(AuditLogSearchRequest request, Pageable pageable) {
        Page<AuditLog> page = auditLogRepository.search(
                normalize(request.action()),
                normalize(request.entityType()),
                request.userId(),
                request.actionType(),
                request.from(),
                request.to(),
                pageable
        );
        return page.map(auditLogMapper::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLogDto> searchCurrentUser(ActionType actionType, LocalDateTime from, LocalDateTime to, Pageable pageable) {
        String email = currentUserEmail()
                .orElseThrow(() -> new AccessDeniedException("User not authenticated"));

        Page<AuditLog> page = auditLogRepository.findByUserEmail(email, actionType, from, to, pageable);
        return page.map(auditLogMapper::toDTO);
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private Optional<Utilisateur> resolveUser(String email) {
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }
        return utilisateurRepository.findByEmail(email);
    }

    private Optional<String> currentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            return Optional.empty();
        }
        String name = authentication.getName().trim();
        return name.isEmpty() ? Optional.empty() : Optional.of(name);
    }
}
