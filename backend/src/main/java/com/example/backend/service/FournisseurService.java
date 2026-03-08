package com.example.backend.service;

import com.example.backend.audit.AuditAction;
import com.example.backend.audit.ActionType;
import com.example.backend.dto.DepenseResponse;
import com.example.backend.dto.FournisseurDto;
import com.example.backend.dto.FournisseurLedgerItemDto;
import com.example.backend.dto.FournisseurSummaryDto;
import com.example.backend.dto.MonthlyDepenseDto;
import com.example.backend.dto.PaiementDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface FournisseurService {

    @AuditAction(action = "FOURNISSEUR_CREATE", entityType = "Fournisseur", type = ActionType.CREATE)
    FournisseurDto createFournisseur(FournisseurDto dto, Long entrepriseId);

    @AuditAction(action = "FOURNISSEUR_UPDATE", entityType = "Fournisseur", type = ActionType.UPDATE)
    FournisseurDto updateFournisseur(Long id, FournisseurDto dto, Long entrepriseId);

    FournisseurDto getFournisseurById(Long id, Long entrepriseId);

    Page<FournisseurDto> getAllFournisseursByEntreprise(Long entrepriseId, Pageable pageable);

    @AuditAction(action = "FOURNISSEUR_DELETE", entityType = "Fournisseur", type = ActionType.DELETE)
    void deleteFournisseur(Long id, Long entrepriseId);

    double calculateTotalDepenses(Long fournisseurId, Long entrepriseId);

    double calculateTotalPaid(Long fournisseurId, Long entrepriseId);

    double calculateRemainingBalance(Long fournisseurId, Long entrepriseId);

    List<DepenseResponse> getUnpaidDepenses(Long fournisseurId, Long entrepriseId);

    List<FournisseurLedgerItemDto> getFournisseurLedger(Long fournisseurId, Long entrepriseId);

    @AuditAction(action = "FOURNISSEUR_PAYMENT_CREATE", entityType = "Paiement", type = ActionType.CREATE)
    PaiementDto recordPayment(Long fournisseurId, PaiementDto dto, Long entrepriseId);

    List<PaiementDto> getPaymentsByFournisseur(Long fournisseurId, Long entrepriseId);

    @AuditAction(action = "FOURNISSEUR_PAYMENT_DELETE", entityType = "Paiement", type = ActionType.DELETE)
    void deletePayment(Long paymentId, Long entrepriseId);

    @AuditAction(action = "FOURNISSEUR_ACTIVATE", entityType = "Fournisseur", type = ActionType.UPDATE)
    FournisseurDto activateFournisseur(Long id, Long entrepriseId);

    @AuditAction(action = "FOURNISSEUR_DEACTIVATE", entityType = "Fournisseur", type = ActionType.UPDATE)
    FournisseurDto deactivateFournisseur(Long id, Long entrepriseId);

    Page<FournisseurDto> searchFournisseurs(String keyword, Long entrepriseId, Pageable pageable);

    List<FournisseurSummaryDto> getTopFournisseursByDepenses(Long entrepriseId);

    List<MonthlyDepenseDto> getMonthlyDepensesByFournisseur(Long fournisseurId, Long entrepriseId);
}
