package com.example.backend.service.impl;

import com.example.backend.dto.FactureDto;
import com.example.backend.audit.AuditAction;
import com.example.backend.audit.ActionType;
import com.example.backend.entitie.Entreprise;
import com.example.backend.entitie.Facture;
import com.example.backend.entitie.Utilisateur;
import com.example.backend.entitie.enumuration.StatutFacture;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.mapper.FactureMapper;
import com.example.backend.repository.EntrepriseRepository;
import com.example.backend.repository.FactureRepository;
import com.example.backend.repository.UtilisateurRepository;
import com.example.backend.service.FactureService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class FactureServiceImpl implements FactureService {

    private final FactureRepository factureRepository;
    private final FactureMapper factureMapper;
    private final UtilisateurRepository utilisateurRepository;
    private final EntrepriseRepository entrepriseRepository;

    public FactureServiceImpl(FactureRepository factureRepository,
                              FactureMapper factureMapper,
                              UtilisateurRepository utilisateurRepository,
                              EntrepriseRepository entrepriseRepository) {
        this.factureRepository = factureRepository;
        this.factureMapper = factureMapper;
        this.utilisateurRepository = utilisateurRepository;
        this.entrepriseRepository = entrepriseRepository;
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN','SECRETAIRE','COMPTABLE')")
    public Page<FactureDto> listFactures(int page, int size) {
        Entreprise entreprise = resolveCurrentEntreprise();
        return factureRepository.findByDevis_Client_Entreprise_Id(entreprise.getId(), PageRequest.of(page, size))
                .map(factureMapper::toDTO);
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN','SECRETAIRE','COMPTABLE')")
    public FactureDto getFacture(Long id) {
        Entreprise entreprise = resolveCurrentEntreprise();
        Facture facture = factureRepository.findByIdAndDevis_Client_Entreprise_Id(id, entreprise.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Facture not found"));
        return factureMapper.toDTO(facture);
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN','SECRETAIRE','COMPTABLE')")
    @AuditAction(action = "FACTURE_SEND_EMAIL", entityType = "Facture", type = ActionType.UPDATE)
    public void sendFactureEmail(Long id) {
        // Placeholder: implement when email + PDF facture generation is ready
        Entreprise entreprise = resolveCurrentEntreprise();
        factureRepository.findByIdAndDevis_Client_Entreprise_Id(id, entreprise.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Facture not found"));
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN','COMPTABLE')")
    @AuditAction(action = "FACTURE_MARK_PAID", entityType = "Facture", type = ActionType.UPDATE)
    public FactureDto markAsPaid(Long id) {
        Entreprise entreprise = resolveCurrentEntreprise();
        Facture facture = factureRepository.findByIdAndDevis_Client_Entreprise_Id(id, entreprise.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Facture not found"));

        facture.setPayee(Boolean.TRUE);
        facture.setStatututFacture(StatutFacture.PAYEE);

        Facture saved = factureRepository.save(facture);
        return factureMapper.toDTO(saved);
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
}
