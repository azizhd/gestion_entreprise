package com.example.backend.service.impl;

import com.example.backend.dto.DepenseDecisionRequest;
import com.example.backend.audit.AuditAction;
import com.example.backend.audit.ActionType;
import com.example.backend.dto.DepenseRequest;
import com.example.backend.dto.DepenseResponse;
import com.example.backend.entitie.Depense;
import com.example.backend.entitie.Entreprise;
import com.example.backend.entitie.Fournisseur;
import com.example.backend.entitie.Tache;
import com.example.backend.entitie.Utilisateur;
import com.example.backend.entitie.enumuration.NotificationType;
import com.example.backend.entitie.enumuration.StatusDepense;
import com.example.backend.entitie.enumuration.TypeRole;
import com.example.backend.exception.BusinessException;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.mapper.DepenseMapper;
import com.example.backend.repository.DepenseRepository;
import com.example.backend.repository.EntrepriseRepository;
import com.example.backend.repository.FournisseurRepository;
import com.example.backend.repository.TacheRepository;
import com.example.backend.repository.UtilisateurRepository;
import com.example.backend.service.DepenseService;
import com.example.backend.service.NotificationService;
import java.time.LocalDateTime;
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
public class DepenseServiceImpl implements DepenseService {

    private final DepenseRepository depenseRepository;
    private final TacheRepository tacheRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final EntrepriseRepository entrepriseRepository;
    private final FournisseurRepository fournisseurRepository;
    private final DepenseMapper depenseMapper;
    private final NotificationService notificationService;

    public DepenseServiceImpl(DepenseRepository depenseRepository,
                              TacheRepository tacheRepository,
                              UtilisateurRepository utilisateurRepository,
                              EntrepriseRepository entrepriseRepository,
                              FournisseurRepository fournisseurRepository,
                              DepenseMapper depenseMapper,
                              NotificationService notificationService) {
        this.depenseRepository = depenseRepository;
        this.tacheRepository = tacheRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.entrepriseRepository = entrepriseRepository;
        this.fournisseurRepository = fournisseurRepository;
        this.depenseMapper = depenseMapper;
        this.notificationService = notificationService;
    }

    @Override
    @AuditAction(action = "DEPENSE_SUBMIT", entityType = "Depense", type = ActionType.CREATE)
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE','SECRETAIRE','COMPTABLE')")
    public DepenseResponse submitExpense(Long taskId, DepenseRequest request) {
        Utilisateur current = resolveCurrentUser();
        Entreprise entreprise = resolveEntrepriseFromUser(current);
        Tache tache = tacheRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));
        ensureSameEntreprise(tache, entreprise.getId());
        ensureUserAssigned(tache, current);

        Depense depense = new Depense();
        depense.setDescription(request.getDescription());
        depense.setMontant(request.getMontant());
        depense.setDate(request.getDate());
        depense.setStatusDepense(StatusDepense.PENDING);
        depense.setTache(tache);
        depense.setCreatedBy(current);

        Depense saved = depenseRepository.save(depense);
        refreshFinancials(tache);
        
        // Notify admins of the same entreprise
        List<Utilisateur> admins = new ArrayList<>(utilisateurRepository.findByEntreprise_IdAndRole(
            entreprise.getId(),
            TypeRole.ROLE_ADMIN
        ));
        List<Utilisateur> secretaires = new ArrayList<>(utilisateurRepository.findByEntreprise_IdAndRole(
            entreprise.getId(),
            TypeRole.ROLE_SECRETAIRE
        ));
        Utilisateur manager = entreprise.getManager();
        if (manager != null
                && manager.getRole() == TypeRole.ROLE_ADMIN
                && admins.stream().noneMatch(u -> Objects.equals(u.getId(), manager.getId()))) {
            admins.add(manager);
        }
        if (!admins.isEmpty() || !secretaires.isEmpty()) {
            List<Long> adminIds = admins.stream().map(Utilisateur::getId).collect(Collectors.toList());
            List<Long> secrIds = secretaires.stream().map(Utilisateur::getId).collect(Collectors.toList());
            adminIds.addAll(secrIds);
            String title = "Nouvelle dépense en attente";
            String message = String.format("Une nouvelle dépense de %.2f € a été soumise par %s pour la tâche: %s",
                    depense.getMontant(),
                    current.getNom(),
                    tache.getDescription()
            );
            notificationService.sendInAppNotificationToUsers(adminIds, title, message, NotificationType.EXPENSE, saved.getId(), "DEPENSE");
        }
        
        return depenseMapper.toResponse(saved);
    }

    @Override
    @AuditAction(action = "DEPENSE_SUBMIT_FOR_FOURNISSEUR", entityType = "Depense", type = ActionType.CREATE)
    @PreAuthorize("hasAnyRole('ADMIN','COMPTABLE')")
    public DepenseResponse submitExpenseForFournisseur(Long fournisseurId, Long entrepriseId, DepenseRequest request) {
        Utilisateur current = resolveCurrentUser();
        Entreprise entreprise = resolveEntrepriseFromUser(current);
        if (!Objects.equals(entreprise.getId(), entrepriseId.intValue())) {
            throw new SecurityException("Accès refusé : entreprise différente");
        }

        Fournisseur fournisseur = fournisseurRepository.findById(fournisseurId)
                .orElseThrow(() -> new ResourceNotFoundException("Fournisseur introuvable"));
        if (fournisseur.getEntreprise() == null || !Objects.equals(fournisseur.getEntreprise().getId(), entrepriseId.intValue())) {
            throw new SecurityException("Accès refusé : entreprise différente");
        }

        Depense depense = new Depense();
        depense.setDescription(request.getDescription());
        depense.setMontant(request.getMontant());
        depense.setDate(request.getDate());
        depense.setStatusDepense(StatusDepense.APPROVED);
        depense.setFournisseur(fournisseur);
        depense.setCreatedBy(current);

        Depense saved = depenseRepository.save(depense);
        return depenseMapper.toResponse(saved);
    }

    @Override
    @AuditAction(action = "DEPENSE_APPROVE_OR_DECLINE", entityType = "Depense", type = ActionType.UPDATE)
    @PreAuthorize("hasAnyRole('ADMIN','COMPTABLE')")
    public DepenseResponse approveOrDecline(Long depenseId, DepenseDecisionRequest request) {
        Utilisateur current = resolveCurrentUser();
        Depense depense = depenseRepository.findById(depenseId)
                .orElseThrow(() -> new ResourceNotFoundException("Dépense introuvable"));
        if (depense.getTache() == null) {
            throw new BusinessException("La dépense n'est liée à aucune tâche");
        }

        Entreprise entreprise = depense.getTache().getEntreprise();
        if (entreprise == null) {
            throw new BusinessException("Aucune entreprise associée à la tâche de la dépense");
        }
        // If the admin has an entreprise, enforce it matches; if not, rely on task's entreprise
        if (current.getEntreprise() != null && !Objects.equals(current.getEntreprise().getId(), entreprise.getId())) {
            throw new SecurityException("Accès refusé : entreprise différente");
        }

        if (depense.getStatusDepense() != StatusDepense.PENDING) {
            throw new BusinessException("La dépense a déjà été traitée");
        }

        depense.setStatusDepense(request.getDecisionStatus());
        depense.setApprovedBy(current);
        depense.setApprovalDate(LocalDateTime.now());
        depense.setRejectionReason(StatusDepense.DECLINED.equals(request.getDecisionStatus()) ? request.getRejectionReason() : null);

        Depense saved = depenseRepository.save(depense);
        refreshFinancials(depense.getTache());
        return depenseMapper.toResponse(saved);
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE','SECRETAIRE','COMPTABLE')")
    public Page<DepenseResponse> listByTask(Long taskId, int page, int size) {
        Utilisateur current = resolveCurrentUser();
        Tache tache = tacheRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));

        Entreprise entreprise = tache.getEntreprise();
        if (entreprise == null) {
            throw new BusinessException("Aucune entreprise associée à cette tâche");
        }
        if (!isAdmin(current)) {
            ensureSameEntreprise(tache, entreprise.getId());
        } else if (current.getEntreprise() != null && !Objects.equals(current.getEntreprise().getId(), entreprise.getId())) {
            throw new SecurityException("Accès refusé : entreprise différente");
        }

        if (isAdmin(current) || isComptable(current)) {
            return depenseRepository.findByTache_IdAndTache_Entreprise_Id(taskId, entreprise.getId(), PageRequest.of(page, size))
                    .map(depenseMapper::toResponse);
        }

        ensureUserAssigned(tache, current);
        return depenseRepository.findByTache_IdAndTache_Entreprise_IdAndTache_Utilisateurs_Id(taskId, entreprise.getId(), current.getId(), PageRequest.of(page, size))
                .map(depenseMapper::toResponse);
    }

    private void refreshFinancials(Tache tache) {
        if (tache.getEntreprise() == null || tache.getId() == null) {
            return;
        }
        Integer entrepriseId = tache.getEntreprise().getId();
        tache.setTotalApprovedExpenses(depenseRepository.sumByTaskAndStatus(tache.getId(), entrepriseId, StatusDepense.APPROVED));
        tache.setTotalPendingExpenses(depenseRepository.sumByTaskAndStatus(tache.getId(), entrepriseId, StatusDepense.PENDING));
        tache.setTotalDeclinedExpenses(depenseRepository.sumByTaskAndStatus(tache.getId(), entrepriseId, StatusDepense.DECLINED));
        tacheRepository.save(tache);
    }

    private void ensureSameEntreprise(Tache tache, Integer entrepriseId) {
        if (tache.getEntreprise() == null || !Objects.equals(tache.getEntreprise().getId(), entrepriseId)) {
            throw new SecurityException("Accès refusé : entreprise différente");
        }
    }

    private void ensureUserAssigned(Tache tache, Utilisateur utilisateur) {
        boolean assigned = tache.getUtilisateurs().stream().anyMatch(u -> Objects.equals(u.getId(), utilisateur.getId()));
        if (!assigned && !isAdmin(utilisateur)) {
            throw new SecurityException("Vous n'êtes pas assigné à cette tâche");
        }
    }

    private Utilisateur resolveCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new SecurityException("No authenticated user");
        }
        return utilisateurRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new SecurityException("Authenticated user not found"));
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

    private boolean isComptable(Utilisateur utilisateur) {
        return utilisateur.getRole() != null && utilisateur.getRole().name().equals("ROLE_COMPTABLE");
    }
}
