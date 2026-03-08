package com.example.backend.service.impl;

import com.example.backend.audit.AuditAction;
import com.example.backend.audit.AuditAction;
import com.example.backend.audit.ActionType;
import com.example.backend.dto.DevisCreateUpdateRequest;
import com.example.backend.dto.DevisDto;
import com.example.backend.dto.FactureDto;
import com.example.backend.dto.LigneDevisRequest;
import com.example.backend.entitie.Client;
import com.example.backend.entitie.Devis;
import com.example.backend.entitie.Entreprise;
import com.example.backend.entitie.Facture;
import com.example.backend.entitie.LigneDevis;
import com.example.backend.entitie.Utilisateur;
import com.example.backend.mapper.FactureMapper;
import com.example.backend.entitie.enumuration.NotificationType;
import com.example.backend.entitie.enumuration.StatutDevis;
import com.example.backend.entitie.enumuration.StatutFacture;
import com.example.backend.entitie.enumuration.TypeRole;
import com.example.backend.exception.BusinessException;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.mapper.DevisMapper;
import com.example.backend.repository.ClientRepository;
import com.example.backend.repository.DevisRepository;
import com.example.backend.repository.EntrepriseRepository;
import com.example.backend.repository.FactureRepository;
import com.example.backend.repository.UtilisateurRepository;
import com.example.backend.service.DevisService;
import com.example.backend.service.NotificationService;

import org.aspectj.lang.annotation.Pointcut;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class DevisServiceImpl implements DevisService {

    private final DevisRepository devisRepository;
    private final DevisMapper devisMapper;
    private final ClientRepository clientRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final EntrepriseRepository entrepriseRepository;
    private final FactureRepository factureRepository;
    private final FactureMapper factureMapper;
    private final NotificationService notificationService;

    public DevisServiceImpl(DevisRepository devisRepository,
                            DevisMapper devisMapper,
                            ClientRepository clientRepository,
                            UtilisateurRepository utilisateurRepository,
                            EntrepriseRepository entrepriseRepository,
                            FactureRepository factureRepository,
                            FactureMapper factureMapper,
                            NotificationService notificationService) {
        this.devisRepository = devisRepository;
        this.devisMapper = devisMapper;
        this.clientRepository = clientRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.entrepriseRepository = entrepriseRepository;
        this.factureRepository = factureRepository;
        this.factureMapper = factureMapper;
        this.notificationService = notificationService;
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN','SECRETAIRE')")
    public Page<DevisDto> getAllDevis(int page, int size) {
        Entreprise entreprise = resolveCurrentEntreprise();
        return devisRepository.findByClient_Entreprise_Id(entreprise.getId(), PageRequest.of(page, size))
                .map(devisMapper::toDto);
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN','SECRETAIRE')")
    public DevisDto getDevisById(Long id) {
        Entreprise entreprise = resolveCurrentEntreprise();
        Devis devis = devisRepository.findByIdAndClient_Entreprise_Id(id, entreprise.getId())
            .orElseThrow(() -> new ResourceNotFoundException("Devis not found"));
        return devisMapper.toDto(devis);
    }

    @Override
    @AuditAction(action = "DEVIS_CREATE", entityType = "Devis", type = ActionType.CREATE)
    @PreAuthorize("hasAnyRole('ADMIN','SECRETAIRE')")
    public DevisDto createDevis(DevisCreateUpdateRequest request) {
        Entreprise entreprise = resolveCurrentEntreprise();
        Client client = clientRepository.findByIdAndEntreprise_Id(request.getClientId(), entreprise.getId())
            .orElseThrow(() -> new ResourceNotFoundException("Client not found"));

        Devis devis = new Devis();
        applyRequestToEntity(request, devis, client);
        assignNumero(devis, entreprise.getId());
        computeTotals(devis);

        Devis saved = devisRepository.save(devis);
        return devisMapper.toDto(saved);
    }

    @Override
    @AuditAction(action = "DEVIS_UPDATE", entityType = "Devis", type = ActionType.UPDATE)
    @PreAuthorize("hasAnyRole('ADMIN','SECRETAIRE')")
    public DevisDto updateDevis(Long id, DevisCreateUpdateRequest request) {
        Entreprise entreprise = resolveCurrentEntreprise();
        Devis devis = devisRepository.findByIdAndClient_Entreprise_Id(id, entreprise.getId())
            .orElseThrow(() -> new ResourceNotFoundException("Devis not found"));
        Client client = clientRepository.findByIdAndEntreprise_Id(request.getClientId(), entreprise.getId())
            .orElseThrow(() -> new ResourceNotFoundException("Client not found"));

        applyRequestToEntity(request, devis, client);
        computeTotals(devis);

        Devis saved = devisRepository.save(devis);
        return devisMapper.toDto(saved);
    }

    @Override
    @AuditAction(action = "DEVIS_DELETE", entityType = "Devis", type = ActionType.DELETE)
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteDevis(Long id) {
        Entreprise entreprise = resolveCurrentEntreprise();
        Devis devis = devisRepository.findByIdAndClient_Entreprise_Id(id, entreprise.getId())
            .orElseThrow(() -> new ResourceNotFoundException("Devis not found"));
        devisRepository.delete(devis);
    }

    @Override
    @AuditAction(action = "DEVIS_TRANSFORM", entityType = "Devis", type = ActionType.UPDATE)
    @PreAuthorize("hasRole('ADMIN')")
    public FactureDto transformerEnFacture(Long id) {
        Entreprise entreprise = resolveCurrentEntreprise();
        Devis devis = devisRepository.findByIdAndClient_Entreprise_Id(id, entreprise.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Devis not found"));

        if (Boolean.TRUE.equals(devis.getTransformeEnFacture())) {
            throw new BusinessException("Le devis est déjà transformé en facture");
        }
        if (devis.getFacture() != null) {
            throw new BusinessException("Le devis possède déjà une facture associée");
        }

        String numeroFacture = assignNumeroFacture(entreprise.getId());

        Facture facture = new Facture();
        facture.setNumeroFacture(numeroFacture);
        facture.setReference(devis.getReference() != null ? devis.getReference() : numeroFacture);
        facture.setDate(LocalDateTime.now());
        facture.setDueDate(resolveDueDate(devis.getClient()));
        facture.setMontant(devis.getTotalTtc());
        facture.setTotalHt(devis.getTotalHt());
        facture.setTotalTva(devis.getTotalTva());
        facture.setTotalTtc(devis.getTotalTtc());
        facture.setTvaRate(devis.getTvaRate());
        facture.setPayee(Boolean.FALSE);
        facture.setDevis(devis);
        facture.setStatututFacture(StatutFacture.IMPAYEE);
        Facture savedFacture = factureRepository.save(facture);

        devis.setFacture(savedFacture);
        devis.setTransformeEnFacture(Boolean.TRUE);
        devis.setStatut(StatutDevis.TRANSFORME);
        devisRepository.save(devis);
        notifyStatusChange(entreprise, devis);
        return factureMapper.toDTO(savedFacture);
    }

    private void applyRequestToEntity(DevisCreateUpdateRequest request, Devis devis, Client client) {
        devis.setClient(client);
        devis.setDevisDate(request.getDevisDate() != null ? request.getDevisDate() : LocalDateTime.now());
        devis.setTvaRate(request.getTvaRate() != null ? request.getTvaRate() : 0.2d);
        if (devis.getStatut() == null) {
            devis.setStatut(StatutDevis.BROUILLON);
        }

        List<LigneDevis> lignes = request.getLignes().stream()
                .map(this::toLigneEntity)
                .toList();
        lignes.forEach(l -> l.setDevis(devis));
        devis.setLignesdevis(lignes);
    }

    private LigneDevis toLigneEntity(LigneDevisRequest dto) {
        LigneDevis ligne = new LigneDevis();
        ligne.setId(dto.getId());
        ligne.setDescription(dto.getDescription());
        ligne.setQuantite(dto.getQuantite());
        ligne.setPrixUnitaire(dto.getPrixUnitaire());
        return ligne;
    }

    private void computeTotals(Devis devis) {
        double totalHt = devis.getLignesdevis().stream()
                .peek(l -> l.setTotal(l.getQuantite() * l.getPrixUnitaire()))
                .mapToDouble(LigneDevis::getTotal)
                .sum();
        double tvaRate = devis.getTvaRate() != null ? devis.getTvaRate() : 0.2d;
        double totalTva = totalHt * tvaRate;
        double totalTtc = totalHt + totalTva;

        devis.setTotalHt(totalHt);
        devis.setTotalTva(totalTva);
        devis.setTotalTtc(totalTtc);
        devis.setMontant(totalTtc);
    }

    private void assignNumero(Devis devis, Integer entrepriseId) {
        String yearPrefix = LocalDate.now().getYear() + "-";
        String latestNumero = devisRepository
                .findTopByNumeroDevisStartingWithAndClient_Entreprise_IdOrderByNumeroDevisDesc(yearPrefix, entrepriseId)
                .map(Devis::getNumeroDevis)
                .orElse(null);
        int nextSequence = 1;
        if (latestNumero != null && latestNumero.startsWith(yearPrefix)) {
            String suffix = latestNumero.substring(yearPrefix.length());
            try {
                nextSequence = Integer.parseInt(suffix) + 1;
            } catch (NumberFormatException ignored) {
                nextSequence = 1;
            }
        }
        String formatted = String.format("%s%04d", yearPrefix, nextSequence);
        devis.setNumeroDevis(formatted);
        devis.setReference(formatted);
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
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new SecurityException("No authenticated user");
        }
        return utilisateurRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new SecurityException("Authenticated user not found"));
    }

    private String assignNumeroFacture(Integer entrepriseId) {
        String yearPrefix = LocalDate.now().getYear() + "-";
        String latestNumero = factureRepository
                .findTopByNumeroFactureStartingWithAndDevis_Client_Entreprise_IdOrderByNumeroFactureDesc(yearPrefix, entrepriseId)
                .map(Facture::getNumeroFacture)
                .orElse(null);

        int nextSequence = 1;
        if (latestNumero != null && latestNumero.startsWith(yearPrefix)) {
            String suffix = latestNumero.substring(yearPrefix.length());
            try {
                nextSequence = Integer.parseInt(suffix) + 1;
            } catch (NumberFormatException ignored) {
                nextSequence = 1;
            }
        }
        return String.format("%s%04d", yearPrefix, nextSequence);
    }

    private LocalDate resolveDueDate(Client client) {
        int terms = client.getPaymentTermsDays() != null && client.getPaymentTermsDays() > 0
                ? client.getPaymentTermsDays()
                : 30;
        return LocalDate.now().plusDays(terms);
    }

    private void notifyStatusChange(Entreprise entreprise, Devis devis) {
        List<Long> adminIds = utilisateurRepository.findByEntreprise_IdAndRole(entreprise.getId(), TypeRole.ROLE_ADMIN)
                .stream().map(Utilisateur::getId).toList();
        List<Long> secretaireIds = utilisateurRepository.findByEntreprise_IdAndRole(entreprise.getId(), TypeRole.ROLE_SECRETAIRE)
                .stream().map(Utilisateur::getId).toList();
        List<Long> recipients = new java.util.ArrayList<>();
        recipients.addAll(adminIds);
        secretaireIds.stream().filter(id -> !recipients.contains(id)).forEach(recipients::add);
        if (recipients.isEmpty()) {
            return;
        }
        String title = "Statut devis mis à jour";
        String message = String.format("Devis %s est maintenant %s", devis.getReference(), devis.getStatut());
        notificationService.sendInAppNotificationToUsers(recipients, title, message, NotificationType.DEVIS, devis.getId(), "DEVIS");
    }
}
