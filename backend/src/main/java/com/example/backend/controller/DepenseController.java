package com.example.backend.controller;

import com.example.backend.dto.DepenseDecisionRequest;
import com.example.backend.dto.DepenseRequest;
import com.example.backend.dto.DepenseResponse;
import com.example.backend.service.DepenseService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class DepenseController {

    private final DepenseService depenseService;

    public DepenseController(DepenseService depenseService) {
        this.depenseService = depenseService;
    }

    @PostMapping("/tasks/{taskId}/depenses")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE','SECRETAIRE','COMPTABLE')")
    public ResponseEntity<DepenseResponse> submit(@PathVariable Long taskId, @Valid @RequestBody DepenseRequest request) {
        return ResponseEntity.ok(depenseService.submitExpense(taskId, request));
    }

    @GetMapping("/tasks/{taskId}/depenses")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE','SECRETAIRE','COMPTABLE')")
    public ResponseEntity<Page<DepenseResponse>> list(@PathVariable Long taskId,
                                                      @RequestParam(defaultValue = "0") int page,
                                                      @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(depenseService.listByTask(taskId, page, size));
    }

    @PostMapping("/fournisseurs/{fournisseurId}/depenses")
    @PreAuthorize("hasAnyRole('ADMIN','COMPTABLE')")
    public ResponseEntity<DepenseResponse> createForFournisseur(@PathVariable Long fournisseurId,
                                                                @RequestParam Long entrepriseId,
                                                                @Valid @RequestBody DepenseRequest request) {
        return ResponseEntity.ok(depenseService.submitExpenseForFournisseur(fournisseurId, entrepriseId, request));
    }

    @PatchMapping("/depenses/{id}/decision")
    @PreAuthorize("hasAnyRole('ADMIN','COMPTABLE')")
    public ResponseEntity<DepenseResponse> decide(@PathVariable Long id, @Valid @RequestBody DepenseDecisionRequest request) {
        return ResponseEntity.ok(depenseService.approveOrDecline(id, request));
    }
}
