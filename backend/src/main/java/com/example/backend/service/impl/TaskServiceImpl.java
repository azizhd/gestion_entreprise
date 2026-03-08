package com.example.backend.service.impl;

import com.example.backend.audit.AuditAction;
import com.example.backend.audit.ActionType;
import com.example.backend.dto.TaskProgressUpdateRequest;
import com.example.backend.dto.TaskRequest;
import com.example.backend.dto.TaskResponse;
import com.example.backend.dto.TaskStatusUpdateRequest;
import com.example.backend.entitie.Entreprise;
import com.example.backend.entitie.Tache;
import com.example.backend.entitie.Utilisateur;
import com.example.backend.entitie.enumuration.NotificationType;
import com.example.backend.entitie.enumuration.StatusDepense;
import com.example.backend.entitie.enumuration.StatusTache;
import com.example.backend.entitie.enumuration.TypeRole;
import com.example.backend.exception.BusinessException;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.mapper.TaskMapper;
import com.example.backend.repository.DepenseRepository;
import com.example.backend.repository.EntrepriseRepository;
import com.example.backend.repository.TacheRepository;
import com.example.backend.repository.UtilisateurRepository;
import com.example.backend.service.TaskService;
import com.example.backend.service.NotificationService;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TaskServiceImpl implements TaskService {

    private final TacheRepository tacheRepository;
    private final DepenseRepository depenseRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final EntrepriseRepository entrepriseRepository;
    private final TaskMapper taskMapper;
    private final NotificationService notificationService;

    public TaskServiceImpl(TacheRepository tacheRepository,
                           DepenseRepository depenseRepository,
                           UtilisateurRepository utilisateurRepository,
                           EntrepriseRepository entrepriseRepository,
                           TaskMapper taskMapper,
                           NotificationService notificationService) {
        this.tacheRepository = tacheRepository;
        this.depenseRepository = depenseRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.entrepriseRepository = entrepriseRepository;
        this.taskMapper = taskMapper;
        this.notificationService = notificationService;
    }

    @Override
    @AuditAction(action = "TASK_CREATE", entityType = "Tache", type = ActionType.CREATE)
    @PreAuthorize("hasAnyRole('ADMIN','SECRETAIRE')")
    public TaskResponse create(TaskRequest request) {
        Entreprise entreprise = resolveCurrentEntreprise();
        validateDates(request.getStartDate(), request.getEndDate());

        Tache tache = new Tache();
        tache.setDescription(request.getDescription());
        tache.setDateDebut(request.getStartDate());
        tache.setDateFin(request.getEndDate());
        tache.setStatus(request.getStatus() != null ? request.getStatus() : StatusTache.A_FAIRE);
        tache.setProgress(0);
        tache.setBudget(request.getBudget());
        tache.setEntreprise(entreprise);
        tache.setTotalApprovedExpenses(0d);
        tache.setTotalPendingExpenses(0d);
        tache.setTotalDeclinedExpenses(0d);

        assignEmployees(tache, request.getAssigneeIds(), entreprise.getId());

        Tache saved = tacheRepository.save(tache);
        notifyOperationalRoles(entreprise, saved);
        return mapAndUpdate(saved);
    }

    @Override
    @AuditAction(action = "TASK_UPDATE", entityType = "Tache", type = ActionType.UPDATE)
    @PreAuthorize("hasAnyRole('ADMIN','SECRETAIRE')")
    public TaskResponse update(Long id, TaskRequest request) {
        Entreprise entreprise = resolveCurrentEntreprise();
        Tache tache = tacheRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));
        ensureSameEntreprise(tache, entreprise.getId());
        validateDates(request.getStartDate(), request.getEndDate());

        tache.setDescription(request.getDescription());
        tache.setDateDebut(request.getStartDate());
        tache.setDateFin(request.getEndDate());
        if (request.getStatus() != null) {
            ensureCompletable(tache.getId(), entreprise.getId(), request.getStatus());
            tache.setStatus(request.getStatus());
        }
        tache.setBudget(request.getBudget());

        assignEmployees(tache, request.getAssigneeIds(), entreprise.getId());

        Tache saved = tacheRepository.save(tache);
        return mapAndUpdate(saved);
    }

    @Override
    @AuditAction(action = "TASK_DELETE", entityType = "Tache", type = ActionType.DELETE)
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(Long id) {
        Entreprise entreprise = resolveCurrentEntreprise();
        Tache tache = tacheRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));
        ensureSameEntreprise(tache, entreprise.getId());
        if (depenseRepository.existsByTache_IdAndStatusDepense(id, StatusDepense.APPROVED)) {
            throw new BusinessException("Impossible de supprimer une tâche avec des dépenses approuvées");
        }
        tacheRepository.delete(tache);
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE','SECRETAIRE','COMPTABLE')")
    public TaskResponse getById(Long id) {
        Utilisateur current = resolveCurrentUser();
        Entreprise entreprise = resolveEntrepriseFromUser(current);
        Tache tache = tacheRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));
        ensureSameEntreprise(tache, entreprise.getId());
        if (!isAdmin(current) && tache.getUtilisateurs().stream().noneMatch(u -> Objects.equals(u.getId(), current.getId()))) {
            throw new SecurityException("Accès refusé à cette tâche");
        }
        return mapAndUpdate(tache);
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN','SECRETAIRE','COMPTABLE')")
    public Page<TaskResponse> listForAdmin(int page, int size) {
        Entreprise entreprise = resolveCurrentEntreprise();
        return tacheRepository.findByEntreprise_Id(entreprise.getId(), PageRequest.of(page, size))
                .map(this::mapAndUpdate);
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE','SECRETAIRE','COMPTABLE')")
    public Page<TaskResponse> listForCurrentUser(int page, int size) {
        Utilisateur current = resolveCurrentUser();
        Entreprise entreprise = resolveEntrepriseFromUser(current);
        return tacheRepository.findByUtilisateurs_IdAndEntreprise_Id(current.getId(), entreprise.getId(), PageRequest.of(page, size))
                .map(this::mapAndUpdate);
    }

    @Override
    @AuditAction(action = "TASK_UPDATE_STATUS", entityType = "Tache", type = ActionType.UPDATE)
    @PreAuthorize("hasAnyRole('ADMIN','SECRETAIRE')")
    public TaskResponse updateStatus(Long id, TaskStatusUpdateRequest request) {
        Entreprise entreprise = resolveCurrentEntreprise();
        Tache tache = tacheRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));
        ensureSameEntreprise(tache, entreprise.getId());
        ensureCompletable(tache.getId(), entreprise.getId(), request.getStatus());
        tache.setStatus(request.getStatus());
        if (StatusTache.TERMINE.equals(request.getStatus())) {
            tache.setProgress(100);
        }
        return mapAndUpdate(tacheRepository.save(tache));
    }

    @Override
    @AuditAction(action = "TASK_UPDATE_PROGRESS", entityType = "Tache", type = ActionType.UPDATE)
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE','SECRETAIRE','COMPTABLE')")
    public TaskResponse updateProgress(Long id, TaskProgressUpdateRequest request) {
        Utilisateur current = resolveCurrentUser();
        Entreprise entreprise = resolveEntrepriseFromUser(current);
        Tache tache = tacheRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));
        ensureSameEntreprise(tache, entreprise.getId());
        if (!isAdmin(current) && tache.getUtilisateurs().stream().noneMatch(u -> Objects.equals(u.getId(), current.getId()))) {
            throw new SecurityException("Vous n'êtes pas assigné à cette tâche");
        }
        tache.setProgress(request.getProgress());
        if (request.getProgress() != null && request.getProgress() == 100) {
            ensureCompletable(tache.getId(), entreprise.getId(), StatusTache.TERMINE);
            tache.setStatus(StatusTache.TERMINE);
        } else if (request.getProgress() != null && request.getProgress() > 0 && StatusTache.A_FAIRE.equals(tache.getStatus())) {
            tache.setStatus(StatusTache.EN_COURS);
        }
        return mapAndUpdate(tacheRepository.save(tache));
    }

    private void validateDates(LocalDate start, LocalDate end) {
        if (start != null && end != null && end.isBefore(start)) {
            throw new BusinessException("La date de fin doit être postérieure à la date de début");
        }
    }
    
    @AuditAction(action = "TASK_ASSIGN_EMPLOYEES", entityType = "Tache", type = ActionType.UPDATE)
    private void assignEmployees(Tache tache, List<Long> assigneeIds, Integer entrepriseId) {
        tache.getUtilisateurs().clear();
        if (assigneeIds == null || assigneeIds.isEmpty()) {
            return;
        }

        List<Utilisateur> assignees = utilisateurRepository.findAllById(assigneeIds);
        List<Utilisateur> sameEntreprise = assignees.stream()
                .filter(u -> u.getEntreprise() != null && Objects.equals(u.getEntreprise().getId(), entrepriseId))
                .toList();

        if (sameEntreprise.size() != assigneeIds.size()) {
            throw new BusinessException("Tous les employés doivent appartenir à la même entreprise");
        }

        tache.setUtilisateurs(new ArrayList<>(sameEntreprise));

        // Keep the inverse side in sync so the owning side (tache) persists the join rows cleanly
        sameEntreprise.forEach(u -> {
            if (u.getTaches() == null) {
                u.setTaches(new ArrayList<>());
            }
            if (!u.getTaches().contains(tache)) {
                u.getTaches().add(tache);
            }
        });

        if (StatusTache.A_FAIRE.equals(tache.getStatus())) {
            tache.setStatus(StatusTache.ASSIGNEE);
        }
    }

    private void ensureSameEntreprise(Tache tache, Integer entrepriseId) {
        if (tache.getEntreprise() == null || !Objects.equals(tache.getEntreprise().getId(), entrepriseId)) {
            throw new SecurityException("Accès refusé : entreprise différente");
        }
    }

    private void ensureCompletable(Long tacheId, Integer entrepriseId, StatusTache targetStatus) {
        if (StatusTache.TERMINE.equals(targetStatus) && depenseRepository.existsByTache_IdAndStatusDepense(tacheId, StatusDepense.PENDING)) {
            throw new BusinessException("Impossible de terminer la tâche avec des dépenses en attente");
        }
    }

    private TaskResponse mapAndUpdate(Tache tache) {
        Tache adjusted = markOverdueIfNeeded(tache);
        refreshFinancials(adjusted);
        Tache saved = tacheRepository.save(adjusted);
        return taskMapper.toResponse(saved);
    }

    private Tache markOverdueIfNeeded(Tache tache) {
        if (tache.getDateFin() != null
                && tache.getStatus() != null
                && !StatusTache.TERMINE.equals(tache.getStatus())
                && tache.getDateFin().isBefore(LocalDate.now())) {
            tache.setStatus(StatusTache.EN_RETARD);
        }
        return tache;
    }

    private void refreshFinancials(Tache tache) {
        if (tache.getId() == null || tache.getEntreprise() == null) {
            tache.setTotalApprovedExpenses(0d);
            tache.setTotalPendingExpenses(0d);
            tache.setTotalDeclinedExpenses(0d);
            return;
        }
        Integer entrepriseId = tache.getEntreprise().getId();
        tache.setTotalApprovedExpenses(depenseRepository.sumByTaskAndStatus(tache.getId(), entrepriseId, StatusDepense.APPROVED));
        tache.setTotalPendingExpenses(depenseRepository.sumByTaskAndStatus(tache.getId(), entrepriseId, StatusDepense.PENDING));
        tache.setTotalDeclinedExpenses(depenseRepository.sumByTaskAndStatus(tache.getId(), entrepriseId, StatusDepense.DECLINED));
    }

    private Utilisateur resolveCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new SecurityException("No authenticated user");
        }
        return utilisateurRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new SecurityException("Authenticated user not found"));
    }

    private Entreprise resolveCurrentEntreprise() {
        Utilisateur current = resolveCurrentUser();
        return resolveEntrepriseFromUser(current);
    }

    private Entreprise resolveEntrepriseFromUser(Utilisateur utilisateur) {
        if (utilisateur.getEntreprise() == null) {
            return entrepriseRepository.findByManager_Id(utilisateur.getId())
                    .orElseThrow(() -> new SecurityException("Entreprise introuvable pour l'utilisateur"));
        }
        return entrepriseRepository.findById(utilisateur.getEntreprise().getId())
                .orElseThrow(() -> new SecurityException("Entreprise introuvable pour l'utilisateur"));
    }

    private boolean isAdmin(Utilisateur utilisateur) {
        return utilisateur.getRole() != null && utilisateur.getRole().name().equals("ROLE_ADMIN");
    }

    private void notifyOperationalRoles(Entreprise entreprise, Tache tache) {
        List<Long> adminIds = utilisateurRepository.findByEntreprise_IdAndRole(entreprise.getId(), TypeRole.ROLE_ADMIN)
                .stream()
                .map(Utilisateur::getId)
                .toList();
        List<Long> secretaireIds = utilisateurRepository.findByEntreprise_IdAndRole(entreprise.getId(), TypeRole.ROLE_SECRETAIRE)
                .stream()
                .map(Utilisateur::getId)
                .toList();

        List<Long> recipients = new java.util.ArrayList<>();
        recipients.addAll(adminIds);
        secretaireIds.stream().filter(id -> !recipients.contains(id)).forEach(recipients::add);

        if (recipients.isEmpty()) {
            return;
        }

        String title = "Nouvelle tâche créée";
        String message = String.format("%s (échéance %s)",
                tache.getDescription(),
                tache.getDateFin() != null ? tache.getDateFin().toString() : "non définie");
        notificationService.sendInAppNotificationToUsers(recipients, title, message, NotificationType.TASK, tache.getId(), "TACHE");
    }
}
