package com.example.backend.service.impl;

import com.example.backend.audit.AuditAction;
import com.example.backend.dto.EntrepriseDto;
import com.example.backend.audit.AuditAction;
import com.example.backend.audit.ActionType;
import com.example.backend.entitie.Abonnement;
import com.example.backend.entitie.Entreprise;
import com.example.backend.entitie.Utilisateur;
import com.example.backend.entitie.enumuration.AbonnementType;
import com.example.backend.mapper.EntrepriseMapper;
import com.example.backend.repository.EntrepriseRepository;
import com.example.backend.repository.UtilisateurRepository;
import com.example.backend.service.EntrepriseService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

@Service
@Transactional
public class EntrepriseServiceImpl implements EntrepriseService {

    private final EntrepriseRepository entrepriseRepository;
    private final EntrepriseMapper entrepriseMapper;
    private final UtilisateurRepository utilisateurRepository;

    public EntrepriseServiceImpl(EntrepriseRepository entrepriseRepository,
                                 EntrepriseMapper entrepriseMapper,
                                 UtilisateurRepository utilisateurRepository) {
        this.entrepriseRepository = entrepriseRepository;
        this.entrepriseMapper = entrepriseMapper;
        this.utilisateurRepository = utilisateurRepository;
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public List<EntrepriseDto> getAll() {
        Utilisateur currentUser = resolveCurrentUser();
        Entreprise entreprise = findEntrepriseForUser(currentUser);
        if (entreprise == null) {
            return List.of();
        }
        return List.of(entrepriseMapper.toDTO(entreprise));
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public EntrepriseDto getById(Integer id) {
        Utilisateur currentUser = resolveCurrentUser();
        Entreprise entreprise = entrepriseRepository.findById(id).orElse(null);
        if (entreprise == null) {
            return null;
        }
        if (!isUserRelatedToEntreprise(currentUser, entreprise)) {
            throw new SecurityException("Access denied to this entreprise");
        }
        return entrepriseMapper.toDTO(entreprise);
    }

    @Override
    @AuditAction(action = "ENTREPRISE_CREATE", entityType = "Entreprise", type = ActionType.CREATE)
    @PreAuthorize("hasRole('ADMIN')")
    public EntrepriseDto create(EntrepriseDto dto) {
        Entreprise entreprise = new Entreprise();
        applyDtoToEntity(dto, entreprise);
        Entreprise saved = entrepriseRepository.save(entreprise);
        return entrepriseMapper.toDTO(saved);
    }

    @Override
    @AuditAction(action = "ENTREPRISE_UPDATE", entityType = "Entreprise", type = ActionType.UPDATE)
    @PreAuthorize("hasRole('ADMIN')")
    public EntrepriseDto update(Integer id, EntrepriseDto dto) {
        Utilisateur currentUser = resolveCurrentUser();
        Optional<Entreprise> existingOpt = entrepriseRepository.findById(id);
        if (existingOpt.isEmpty()) {
            return null;
        }
        Entreprise existing = existingOpt.get();
        if (existing.getManager() == null || !existing.getManager().getId().equals(currentUser.getId())) {
            throw new SecurityException("Only the manager can update this entreprise");
        }
        applyDtoToEntity(dto, existing);
        Entreprise saved = entrepriseRepository.save(existing);
        return entrepriseMapper.toDTO(saved);
    }

    @Override
    @AuditAction(action = "ENTREPRISE_DELETE", entityType = "Entreprise", type = ActionType.DELETE)
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(Integer id) {
        Utilisateur currentUser = resolveCurrentUser();
        Entreprise existing = entrepriseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Entreprise not found"));
        if (existing.getManager() == null || !existing.getManager().getId().equals(currentUser.getId())) {
            throw new SecurityException("Only the manager can delete this entreprise");
        }
        entrepriseRepository.delete(existing);
    }

    private void applyDtoToEntity(EntrepriseDto dto, Entreprise entity) {
        entity.setNom(dto.getNom());
        entity.setLogo(dto.getLogo());
        entity.setEmail(dto.getEmail());
        entity.setTelephone(dto.getTelephone());
        entity.setLocation(dto.getLocation());

        Utilisateur manager = resolveCurrentUser();

        entrepriseRepository.findByManager_Id(manager.getId())
                .filter(existing -> entity.getId() == 0 || existing.getId() != entity.getId())
                .ifPresent(existing -> {
                    AbonnementType tier = existing.getAbonnement() != null ? existing.getAbonnement().getAbonnementType() : null;
                    if (tier == null || (tier != AbonnementType.AVANCE && tier != AbonnementType.PREMIUM)) {
                        throw new IllegalArgumentException("Manager already manages an entreprise; requires AVANCE or PREMIUM abonnement on the existing entreprise to manage multiple.");
                    }
                });

        if (entity.getAbonnement() == null) {
            Abonnement abonnement = new Abonnement();
            abonnement.setAbonnementType(AbonnementType.FREE_TRIAL_15DAYS);
            abonnement.setDateDebut(LocalDateTime.now());
            abonnement.setDateFin(LocalDateTime.now().plusDays(15));
            abonnement.setStatus(Boolean.TRUE);
            entity.setAbonnement(abonnement);
        }

        entity.setManager(manager);
    }

    private Utilisateur resolveCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new IllegalArgumentException("No authenticated user");
        }
        return utilisateurRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new IllegalArgumentException("Authenticated user not found"));
    }

    private Entreprise findEntrepriseForUser(Utilisateur user) {
        return entrepriseRepository.findByManager_Id(user.getId())
                .orElseGet(() -> {
                    if (user.getEntreprise() == null) {
                        return null;
                    }
                    return entrepriseRepository.findById(user.getEntreprise().getId()).orElse(null);
                });
    }

    private boolean isUserRelatedToEntreprise(Utilisateur user, Entreprise entreprise) {
        if (entreprise.getManager() != null && entreprise.getManager().getId() != null
                && entreprise.getManager().getId().equals(user.getId())) {
            return true;
        }
        return user.getEntreprise() != null && entreprise.getId() == user.getEntreprise().getId();
    }
}
