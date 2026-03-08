package com.example.backend.service.impl;

import com.example.backend.dto.RendezVousDto;
import com.example.backend.dto.RendezVousRequest;
import com.example.backend.dto.RendezVousStatusUpdateRequest;
import com.example.backend.entitie.Client;
import com.example.backend.entitie.Entreprise;
import com.example.backend.entitie.RendezVous;
import com.example.backend.entitie.Utilisateur;
import com.example.backend.entitie.enumuration.NotificationType;
import com.example.backend.entitie.enumuration.RendezVousStatus;
import com.example.backend.entitie.enumuration.TypeRole;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.mapper.RendezVousMapper;
import com.example.backend.repository.ClientRepository;
import com.example.backend.repository.EntrepriseRepository;
import com.example.backend.repository.RendezVousRepository;
import com.example.backend.repository.UtilisateurRepository;
import com.example.backend.service.RendezVousService;
import com.example.backend.service.NotificationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class RendezVousServiceImpl implements RendezVousService {

    private final RendezVousRepository rendezVousRepository;
    private final RendezVousMapper rendezVousMapper;
    private final UtilisateurRepository utilisateurRepository;
    private final EntrepriseRepository entrepriseRepository;
    private final ClientRepository clientRepository;
    private final NotificationService notificationService;

    public RendezVousServiceImpl(RendezVousRepository rendezVousRepository,
                                 RendezVousMapper rendezVousMapper,
                                 UtilisateurRepository utilisateurRepository,
                                 EntrepriseRepository entrepriseRepository,
                                 ClientRepository clientRepository,
                                 NotificationService notificationService) {
        this.rendezVousRepository = rendezVousRepository;
        this.rendezVousMapper = rendezVousMapper;
        this.utilisateurRepository = utilisateurRepository;
        this.entrepriseRepository = entrepriseRepository;
        this.clientRepository = clientRepository;
        this.notificationService = notificationService;
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN','SECRETAIRE')")
    public Page<RendezVousDto> list(int page, int size) {
        Entreprise entreprise = resolveCurrentEntreprise();
        return rendezVousRepository.findByEntreprise_Id(entreprise.getId(), PageRequest.of(page, size))
                .map(rendezVousMapper::toDto);
    }

    @Override
    @PreAuthorize("hasRole('SECRETAIRE')")
    public RendezVousDto create(RendezVousRequest request) {
        Entreprise entreprise = resolveCurrentEntreprise();
        RendezVous entity = rendezVousMapper.toEntity(request);
        entity.setEntreprise(entreprise);
        entity.setCreatedBy(resolveCurrentUser());
        attachClientIfAny(entity, request.getClientId(), entreprise.getId());
        entity.setStatus(RendezVousStatus.PENDING);
        RendezVous saved = rendezVousRepository.save(entity);
        notifyOperationalRoles(entreprise, saved);
        return rendezVousMapper.toDto(saved);
    }

    @Override
    @PreAuthorize("hasRole('SECRETAIRE')")
    public RendezVousDto update(Long id, RendezVousRequest request) {
        Entreprise entreprise = resolveCurrentEntreprise();
        RendezVous existing = rendezVousRepository.findById(id)
            .filter(r -> r.getEntreprise() != null && r.getEntreprise().getId() == entreprise.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Rendez-vous not found"));

        rendezVousMapper.updateEntity(request, existing);
        attachClientIfAny(existing, request.getClientId(), entreprise.getId());
        RendezVous saved = rendezVousRepository.save(existing);
        return rendezVousMapper.toDto(saved);
    }

    @Override
    @PreAuthorize("hasRole('SECRETAIRE')")
    public void delete(Long id) {
        Entreprise entreprise = resolveCurrentEntreprise();
        RendezVous existing = rendezVousRepository.findById(id)
            .filter(r -> r.getEntreprise() != null && r.getEntreprise().getId() == entreprise.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Rendez-vous not found"));
        rendezVousRepository.delete(existing);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public RendezVousDto updateStatus(Long id, RendezVousStatusUpdateRequest request) {
        Entreprise entreprise = resolveCurrentEntreprise();
        RendezVous existing = rendezVousRepository.findById(id)
            .filter(r -> r.getEntreprise() != null && r.getEntreprise().getId() == entreprise.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Rendez-vous not found"));
        existing.setStatus(request.getStatus());
        existing.setStatusNote(request.getNote());
        RendezVous saved = rendezVousRepository.save(existing);
        return rendezVousMapper.toDto(saved);
    }

    private void attachClientIfAny(RendezVous entity, Long clientId, Integer entrepriseId) {
        if (clientId == null) {
            entity.setClient(null);
            return;
        }
        Client client = clientRepository.findByIdAndEntreprise_Id(clientId, entrepriseId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found"));
        rendezVousMapper.attachClient(entity, client);
    }

    private Entreprise resolveCurrentEntreprise() {
        Utilisateur currentUser = resolveCurrentUser();
        return entrepriseRepository.findByManager_Id(currentUser.getId())
                .orElseGet(() -> {
                    if (currentUser.getEntreprise() == null) {
                        throw new SecurityException("Current user is not linked to an entreprise");
                    }
                    return entrepriseRepository.findById(currentUser.getEntreprise().getId())
                            .orElseThrow(() -> new SecurityException("Entreprise not found for current user"));
                });
    }

    private Utilisateur resolveCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new SecurityException("No authenticated user");
        }
        return utilisateurRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new SecurityException("Authenticated user not found"));
    }

    private void notifyOperationalRoles(Entreprise entreprise, RendezVous rendezVous) {
        List<Utilisateur> admins = utilisateurRepository.findByEntreprise_IdAndRole(entreprise.getId(), TypeRole.ROLE_ADMIN);
        List<Utilisateur> secretaires = utilisateurRepository.findByEntreprise_IdAndRole(entreprise.getId(), TypeRole.ROLE_SECRETAIRE);

        List<Long> recipients = admins.stream().map(Utilisateur::getId).toList();
        List<Long> secIds = secretaires.stream().map(Utilisateur::getId).toList();
        recipients = new java.util.ArrayList<>(recipients);
        recipients.addAll(secIds);

        if (recipients.isEmpty()) {
            return;
        }

        String title = "Nouveau rendez-vous";
        String message = String.format("Rendez-vous planifié le %s", rendezVous.getStartDateTime() != null ? rendezVous.getStartDateTime().toString() : "à planifier");
        notificationService.sendInAppNotificationToUsers(recipients, title, message, NotificationType.RENDEZ_VOUS, rendezVous.getId(), "RENDEZ_VOUS");
    }
}
