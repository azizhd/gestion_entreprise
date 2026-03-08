package com.example.backend.service.impl;

import com.example.backend.dto.DepenseResponse;
import com.example.backend.audit.AuditAction;
import com.example.backend.audit.ActionType;
import com.example.backend.dto.FournisseurDto;
import com.example.backend.dto.FournisseurLedgerItemDto;
import com.example.backend.dto.FournisseurSummaryDto;
import com.example.backend.dto.MonthlyDepenseDto;
import com.example.backend.dto.PaiementDto;
import com.example.backend.entitie.Entreprise;
import com.example.backend.entitie.Fournisseur;
import com.example.backend.entitie.Paiement;
import com.example.backend.exception.BusinessException;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.mapper.DepenseMapper;
import com.example.backend.mapper.FournisseurMapper;
import com.example.backend.repository.DepenseRepository;
import com.example.backend.repository.EntrepriseRepository;
import com.example.backend.repository.FournisseurRepository;
import com.example.backend.repository.PaiementRepository;
import com.example.backend.service.FournisseurService;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class FournisseurServiceImpl implements FournisseurService {

    private final FournisseurRepository fournisseurRepository;
    private final EntrepriseRepository entrepriseRepository;
    private final DepenseRepository depenseRepository;
    private final PaiementRepository paiementRepository;
    private final FournisseurMapper fournisseurMapper;
    private final DepenseMapper depenseMapper;

    public FournisseurServiceImpl(FournisseurRepository fournisseurRepository,
                                  EntrepriseRepository entrepriseRepository,
                                  DepenseRepository depenseRepository,
                                  PaiementRepository paiementRepository,
                                  FournisseurMapper fournisseurMapper,
                                  DepenseMapper depenseMapper) {
        this.fournisseurRepository = fournisseurRepository;
        this.entrepriseRepository = entrepriseRepository;
        this.depenseRepository = depenseRepository;
        this.paiementRepository = paiementRepository;
        this.fournisseurMapper = fournisseurMapper;
        this.depenseMapper = depenseMapper;
    }

    @AuditAction(action = "FOURNISSEUR_CREATE", entityType = "Fournisseur", type = ActionType.CREATE)
    @Override
    public FournisseurDto createFournisseur(FournisseurDto dto, Long entrepriseId) {
        Entreprise entreprise = loadEntreprise(entrepriseId);
        Fournisseur fournisseur = fournisseurMapper.toEntity(dto);
        fournisseur.setEntreprise(entreprise);
        if (fournisseur.getActif() == null) {
            fournisseur.setActif(Boolean.TRUE);
        }
        Fournisseur saved = fournisseurRepository.save(fournisseur);
        return fournisseurMapper.toDTO(saved);
    }

    @AuditAction(action = "FOURNISSEUR_UPDATE", entityType = "Fournisseur", type = ActionType.UPDATE)
    @Override
    public FournisseurDto updateFournisseur(Long id, FournisseurDto dto, Long entrepriseId) {
        Fournisseur fournisseur = loadFournisseur(id, entrepriseId);
        fournisseur.setNom(dto.getNom());
        fournisseur.setPrenom(dto.getPrenom());
        fournisseur.setTelephone(dto.getTelephone());
        fournisseur.setEmail(dto.getEmail());
        fournisseur.setDescription(dto.getDescription());
        fournisseur.setActif(dto.getActif());
        fournisseur.setCreditLimit(dto.getCreditLimit());
        fournisseur.setCategory(dto.getCategory());
        Fournisseur saved = fournisseurRepository.save(fournisseur);
        return fournisseurMapper.toDTO(saved);
    }

    @Override
    public FournisseurDto getFournisseurById(Long id, Long entrepriseId) {
        Fournisseur fournisseur = loadFournisseur(id, entrepriseId);
        return fournisseurMapper.toDTO(fournisseur);
    }

    @Override
    public Page<FournisseurDto> getAllFournisseursByEntreprise(Long entrepriseId, Pageable pageable) {
        return fournisseurRepository.findByEntreprise_Id(Math.toIntExact(entrepriseId), pageable)
                .map(fournisseurMapper::toDTO);
    }

    @AuditAction(action = "FOURNISSEUR_DELETE", entityType = "Fournisseur", type = ActionType.DELETE)
    @Override
    public void deleteFournisseur(Long id, Long entrepriseId) {
        Fournisseur fournisseur = loadFournisseur(id, entrepriseId);
        fournisseurRepository.delete(fournisseur);
    }

    @Override
    public double calculateTotalDepenses(Long fournisseurId, Long entrepriseId) {
        loadFournisseur(fournisseurId, entrepriseId);
        return depenseRepository.sumApprovedByFournisseur(fournisseurId, Math.toIntExact(entrepriseId));
    }

    @Override
    public double calculateTotalPaid(Long fournisseurId, Long entrepriseId) {
        loadFournisseur(fournisseurId, entrepriseId);
        return paiementRepository.sumByFournisseur(fournisseurId, Math.toIntExact(entrepriseId));
    }

    @Override
    public double calculateRemainingBalance(Long fournisseurId, Long entrepriseId) {
        double totalDepenses = calculateTotalDepenses(fournisseurId, entrepriseId);
        double totalPaid = calculateTotalPaid(fournisseurId, entrepriseId);
        return totalDepenses - totalPaid;
    }

    @Override
    public List<DepenseResponse> getUnpaidDepenses(Long fournisseurId, Long entrepriseId) {
        loadFournisseur(fournisseurId, entrepriseId);
        return depenseRepository.findUnpaidByFournisseur(fournisseurId, Math.toIntExact(entrepriseId)).stream()
                .map(depenseMapper::toResponse)
                .toList();
    }

    @Override
    public List<FournisseurLedgerItemDto> getFournisseurLedger(Long fournisseurId, Long entrepriseId) {
        Fournisseur fournisseur = loadFournisseur(fournisseurId, entrepriseId);
        List<FournisseurLedgerItemDto> entries = new ArrayList<>();

        depenseRepository.findByFournisseur_IdAndFournisseur_Entreprise_IdOrderByDateAsc(fournisseurId, Math.toIntExact(entrepriseId))
                .forEach(depense -> entries.add(FournisseurLedgerItemDto.builder()
                        .date(depense.getDate())
                        .type(FournisseurLedgerItemDto.EntryType.DEPENSE)
                        .amount(depense.getMontant())
                        .reference(null)
                        .description(depense.getDescription())
                        .build()));

        paiementRepository.findByFournisseur_IdAndEntreprise_Id(fournisseurId, fournisseur.getEntreprise().getId())
            .forEach(pay -> entries.add(FournisseurLedgerItemDto.builder()
                .date(pay.getDate())
                .type(FournisseurLedgerItemDto.EntryType.PAIEMENT)
                .amount(pay.getMontant())
                .reference(null)
                .description(null)
                .build()));

        entries.sort(Comparator.comparing(FournisseurLedgerItemDto::getDate));

        double running = 0d;
        for (FournisseurLedgerItemDto entry : entries) {
            if (entry.getType() == FournisseurLedgerItemDto.EntryType.DEPENSE) {
                running += Optional.ofNullable(entry.getAmount()).orElse(0d);
            } else {
                running -= Optional.ofNullable(entry.getAmount()).orElse(0d);
            }
            entry.setRunningBalance(running);
        }
        return entries;
    }

    @Override
    public PaiementDto recordPayment(Long fournisseurId, PaiementDto dto, Long entrepriseId) {
        Fournisseur fournisseur = loadFournisseur(fournisseurId, entrepriseId);
        if (dto.getFactureId() != null) {
            throw new BusinessException("Un paiement fournisseur ne peut pas être lié à une facture client");
        }
        if (dto.getFournisseurId() != null && !Objects.equals(dto.getFournisseurId(), fournisseurId)) {
            throw new AccessDeniedException("Conflit sur l'identifiant fournisseur");
        }
        if (dto.getEntrepriseId() != null && !Objects.equals(dto.getEntrepriseId(), entrepriseId)) {
            throw new AccessDeniedException("Conflit sur l'identifiant entreprise");
        }
        if (dto.getMontant() == null || dto.getMontant() <= 0) {
            throw new BusinessException("Le montant du paiement doit être positif");
        }
        double remaining = calculateRemainingBalance(fournisseurId, entrepriseId);
        if (dto.getMontant() > remaining) {
            throw new BusinessException("Le paiement dépasse le solde restant");
        }

        Paiement paiement = new Paiement();
        paiement.setMontant(dto.getMontant());
        paiement.setDate(Optional.ofNullable(dto.getDate()).orElse(LocalDate.now()));
        paiement.setModePaiement(dto.getModePaiement());
        paiement.setFournisseur(fournisseur);
        paiement.setEntreprise(fournisseur.getEntreprise());
        paiement.setFacture(null);

        Paiement saved = paiementRepository.save(paiement);
        return PaiementDto.builder()
            .id(saved.getId())
            .montant(saved.getMontant())
            .date(saved.getDate())
            .modePaiement(saved.getModePaiement())
            .fournisseurId(fournisseurId)
            .entrepriseId(entrepriseId)
            .build();
    }

    @Override
    public List<PaiementDto> getPaymentsByFournisseur(Long fournisseurId, Long entrepriseId) {
        Fournisseur fournisseur = loadFournisseur(fournisseurId, entrepriseId);
        return paiementRepository.findByFournisseur_IdAndEntreprise_Id(fournisseurId, fournisseur.getEntreprise().getId())
                .stream()
                .map(p -> PaiementDto.builder()
                        .id(p.getId())
                        .montant(p.getMontant())
                        .date(p.getDate())
                        .modePaiement(p.getModePaiement())
                        .fournisseurId(fournisseurId)
                        .entrepriseId(entrepriseId)
                        .build())
                .toList();
    }

    @AuditAction(action = "FOURNISSEUR_PAYMENT_DELETE", entityType = "Paiement", type = ActionType.DELETE)
    @Override
    public void deletePayment(Long paymentId, Long entrepriseId) {
        Paiement paiement = paiementRepository.findById(paymentId)
            .orElseThrow(() -> new ResourceNotFoundException("Paiement introuvable"));
        if (paiement.getEntreprise() == null || !Objects.equals(paiement.getEntreprise().getId(), entrepriseId.intValue())) {
            throw new AccessDeniedException("Accès refusé");
        }
        paiementRepository.delete(paiement);
    }

    @AuditAction(action = "FOURNISSEUR_ACTIVATE", entityType = "Fournisseur", type = ActionType.UPDATE)
    @Override
    public FournisseurDto activateFournisseur(Long id, Long entrepriseId) {
        Fournisseur fournisseur = loadFournisseur(id, entrepriseId);
        fournisseur.setActif(Boolean.TRUE);
        return fournisseurMapper.toDTO(fournisseurRepository.save(fournisseur));
    }

    @AuditAction(action = "FOURNISSEUR_DEACTIVATE", entityType = "Fournisseur", type = ActionType.UPDATE)
    @Override
    public FournisseurDto deactivateFournisseur(Long id, Long entrepriseId) {
        Fournisseur fournisseur = loadFournisseur(id, entrepriseId);
        fournisseur.setActif(Boolean.FALSE);
        return fournisseurMapper.toDTO(fournisseurRepository.save(fournisseur));
    }

    @Override
    public Page<FournisseurDto> searchFournisseurs(String keyword, Long entrepriseId, Pageable pageable) {
        return fournisseurRepository.search(keyword == null ? "" : keyword, Math.toIntExact(entrepriseId), pageable)
                .map(fournisseurMapper::toDTO);
    }

    @Override
    public List<FournisseurSummaryDto> getTopFournisseursByDepenses(Long entrepriseId) {
        List<Object[]> rows = depenseRepository.topFournisseursByDepense(Math.toIntExact(entrepriseId));
        Map<Long, Double> totals = rows.stream()
                .collect(Collectors.toMap(r -> (Long) r[0], r -> (Double) r[1]));
        List<Fournisseur> fournisseurs = fournisseurRepository.findAllById(totals.keySet());
        return fournisseurs.stream()
                .filter(f -> Objects.equals(f.getEntreprise().getId(), entrepriseId.intValue()))
                .sorted(Comparator.comparing(f -> -totals.getOrDefault(f.getId(), 0d)))
                .map(f -> FournisseurSummaryDto.builder()
                        .id(f.getId())
                        .nom(f.getNom())
                        .prenom(f.getPrenom())
                        .actif(f.getActif())
                        .category(f.getCategory())
                        .creditLimit(f.getCreditLimit())
                        .totalDepenses(totals.getOrDefault(f.getId(), 0d))
                        .totalPaid(paiementRepository.sumByFournisseur(f.getId(), f.getEntreprise().getId()))
                        .remainingBalance(calculateRemainingBalance(f.getId(), entrepriseId))
                        .build())
                .toList();
    }

    @Override
    public List<MonthlyDepenseDto> getMonthlyDepensesByFournisseur(Long fournisseurId, Long entrepriseId) {
        loadFournisseur(fournisseurId, entrepriseId);
        return depenseRepository.monthlyTotalsByFournisseur(fournisseurId, Math.toIntExact(entrepriseId)).stream()
                .map(r -> MonthlyDepenseDto.builder()
                        .month((String) r[0])
                        .total((Double) r[1])
                        .build())
                .toList();
    }

    private Fournisseur loadFournisseur(Long id, Long entrepriseId) {
        Fournisseur fournisseur = fournisseurRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fournisseur introuvable"));
        if (fournisseur.getEntreprise() == null || !Objects.equals(fournisseur.getEntreprise().getId(), entrepriseId.intValue())) {
            throw new AccessDeniedException("Accès refusé");
        }
        return fournisseur;
    }

    private Entreprise loadEntreprise(Long entrepriseId) {
        return entrepriseRepository.findById(entrepriseId.intValue())
                .orElseThrow(() -> new ResourceNotFoundException("Entreprise introuvable"));
    }
}
