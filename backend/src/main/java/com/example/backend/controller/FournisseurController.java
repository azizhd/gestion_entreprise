package com.example.backend.controller;

import com.example.backend.dto.DepenseResponse;
import com.example.backend.dto.FournisseurDto;
import com.example.backend.dto.FournisseurLedgerItemDto;
import com.example.backend.dto.FournisseurSummaryDto;
import com.example.backend.dto.MonthlyDepenseDto;
import com.example.backend.dto.PaiementDto;
import com.example.backend.service.FournisseurService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/fournisseurs")
public class FournisseurController {

    private final FournisseurService fournisseurService;

    public FournisseurController(FournisseurService fournisseurService) {
        this.fournisseurService = fournisseurService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<FournisseurDto> create(@Valid @RequestBody FournisseurDto dto,
                                                 @RequestParam Long entrepriseId) {
        return ResponseEntity.ok(fournisseurService.createFournisseur(dto, entrepriseId));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','COMPTABLE')")
    public ResponseEntity<Page<FournisseurDto>> list(@RequestParam Long entrepriseId,
                                                     @RequestParam(required = false) String keyword,
                                                     Pageable pageable) {
        if (keyword != null && !keyword.isBlank()) {
            return ResponseEntity.ok(fournisseurService.searchFournisseurs(keyword, entrepriseId, pageable));
        }
        return ResponseEntity.ok(fournisseurService.getAllFournisseursByEntreprise(entrepriseId, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE','SECRETAIRE','COMPTABLE')")
    public ResponseEntity<FournisseurDto> get(@PathVariable Long id, @RequestParam Long entrepriseId) {
        return ResponseEntity.ok(fournisseurService.getFournisseurById(id, entrepriseId));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<FournisseurDto> update(@PathVariable Long id,
                                                 @Valid @RequestBody FournisseurDto dto,
                                                 @RequestParam Long entrepriseId) {
        return ResponseEntity.ok(fournisseurService.updateFournisseur(id, dto, entrepriseId));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id, @RequestParam Long entrepriseId) {
        fournisseurService.deleteFournisseur(id, entrepriseId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<FournisseurDto> activate(@PathVariable Long id, @RequestParam Long entrepriseId) {
        return ResponseEntity.ok(fournisseurService.activateFournisseur(id, entrepriseId));
    }

    @PostMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<FournisseurDto> deactivate(@PathVariable Long id, @RequestParam Long entrepriseId) {
        return ResponseEntity.ok(fournisseurService.deactivateFournisseur(id, entrepriseId));
    }

    @GetMapping("/{id}/payments")
    @PreAuthorize("hasAnyRole('ADMIN','COMPTABLE')")
    public ResponseEntity<List<PaiementDto>> listPayments(@PathVariable Long id, @RequestParam Long entrepriseId) {
        return ResponseEntity.ok(fournisseurService.getPaymentsByFournisseur(id, entrepriseId));
    }

    @PostMapping("/{id}/payments")
    @PreAuthorize("hasAnyRole('ADMIN','COMPTABLE')")
    public ResponseEntity<PaiementDto> recordPayment(@PathVariable Long id,
                                                     @Valid @RequestBody PaiementDto dto,
                                                     @RequestParam Long entrepriseId) {
        return ResponseEntity.ok(fournisseurService.recordPayment(id, dto, entrepriseId));
    }

    @DeleteMapping("/{id}/payments/{paymentId}")
    @PreAuthorize("hasAnyRole('ADMIN','COMPTABLE')")
    public ResponseEntity<Void> deletePayment(@PathVariable Long id,
                                              @PathVariable Long paymentId,
                                              @RequestParam Long entrepriseId) {
        // id is used for route clarity; validation handled in service via entrepriseId
        fournisseurService.deletePayment(paymentId, entrepriseId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/ledger")
    @PreAuthorize("hasAnyRole('ADMIN','COMPTABLE')")
    public ResponseEntity<List<FournisseurLedgerItemDto>> ledger(@PathVariable Long id, @RequestParam Long entrepriseId) {
        return ResponseEntity.ok(fournisseurService.getFournisseurLedger(id, entrepriseId));
    }

    @GetMapping("/{id}/summary")
    @PreAuthorize("hasAnyRole('ADMIN','COMPTABLE')")
    public ResponseEntity<FournisseurSummaryDto> summary(@PathVariable Long id, @RequestParam Long entrepriseId) {
        double total = fournisseurService.calculateTotalDepenses(id, entrepriseId);
        double paid = fournisseurService.calculateTotalPaid(id, entrepriseId);
        double remaining = fournisseurService.calculateRemainingBalance(id, entrepriseId);
        FournisseurDto base = fournisseurService.getFournisseurById(id, entrepriseId);
        FournisseurSummaryDto summary = FournisseurSummaryDto.builder()
                .id(base.getId())
                .nom(base.getNom())
                .prenom(base.getPrenom())
                .actif(base.getActif())
                .category(base.getCategory())
                .creditLimit(base.getCreditLimit())
                .totalDepenses(total)
                .totalPaid(paid)
                .remainingBalance(remaining)
                .build();
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/{id}/unpaid-depenses")
    @PreAuthorize("hasAnyRole('ADMIN','COMPTABLE')")
    public ResponseEntity<List<DepenseResponse>> unpaid(@PathVariable Long id, @RequestParam Long entrepriseId) {
        return ResponseEntity.ok(fournisseurService.getUnpaidDepenses(id, entrepriseId));
    }

    @GetMapping("/top")
    @PreAuthorize("hasAnyRole('ADMIN','COMPTABLE')")
    public ResponseEntity<List<FournisseurSummaryDto>> top(@RequestParam Long entrepriseId) {
        return ResponseEntity.ok(fournisseurService.getTopFournisseursByDepenses(entrepriseId));
    }

    @GetMapping("/{id}/monthly-depenses")
    @PreAuthorize("hasAnyRole('ADMIN','COMPTABLE')")
    public ResponseEntity<List<MonthlyDepenseDto>> monthly(@PathVariable Long id, @RequestParam Long entrepriseId) {
        return ResponseEntity.ok(fournisseurService.getMonthlyDepensesByFournisseur(id, entrepriseId));
    }
}
