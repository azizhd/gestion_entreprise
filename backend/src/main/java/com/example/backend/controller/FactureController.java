package com.example.backend.controller;

import com.example.backend.dto.FactureDto;
import com.example.backend.service.FactureService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/factures")
public class FactureController {

    private final FactureService factureService;

    public FactureController(FactureService factureService) {
        this.factureService = factureService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','SECRETAIRE','COMPTABLE')")
    public ResponseEntity<Page<FactureDto>> list(@RequestParam(defaultValue = "0") int page,
                                                 @RequestParam(defaultValue = "20") int size,
                                                 @RequestParam(required = false) String statut) {
        return ResponseEntity.ok(factureService.listFactures(page, size, statut));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SECRETAIRE','COMPTABLE')")
    public ResponseEntity<FactureDto> get(@PathVariable Long id) {
        return ResponseEntity.ok(factureService.getFacture(id));
    }

    @PostMapping("/{id}/send")
    @PreAuthorize("hasAnyRole('ADMIN','SECRETAIRE','COMPTABLE')")
    public ResponseEntity<Void> send(@PathVariable Long id) {
        factureService.sendFactureEmail(id);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/{id}/pay")
    @PreAuthorize("hasAnyRole('ADMIN','COMPTABLE')")
    public ResponseEntity<FactureDto> markPaid(@PathVariable Long id) {
        return ResponseEntity.ok(factureService.markAsPaid(id));
    }
}
