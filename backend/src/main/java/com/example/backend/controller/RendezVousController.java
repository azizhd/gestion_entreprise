package com.example.backend.controller;

import com.example.backend.dto.RendezVousDto;
import com.example.backend.dto.RendezVousRequest;
import com.example.backend.dto.RendezVousStatusUpdateRequest;
import com.example.backend.service.RendezVousService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rendezvous")
public class RendezVousController {

    private final RendezVousService rendezVousService;

    public RendezVousController(RendezVousService rendezVousService) {
        this.rendezVousService = rendezVousService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','SECRETAIRE')")
    public ResponseEntity<Page<RendezVousDto>> list(@RequestParam(defaultValue = "0") int page,
                                                    @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(rendezVousService.list(page, size));
    }

    @PostMapping
    @PreAuthorize("hasRole('SECRETAIRE')")
    public ResponseEntity<RendezVousDto> create(@Valid @RequestBody RendezVousRequest request) {
        return ResponseEntity.ok(rendezVousService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SECRETAIRE')")
    public ResponseEntity<RendezVousDto> update(@PathVariable Long id, @Valid @RequestBody RendezVousRequest request) {
        return ResponseEntity.ok(rendezVousService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SECRETAIRE')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        rendezVousService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RendezVousDto> updateStatus(@PathVariable Long id, @Valid @RequestBody RendezVousStatusUpdateRequest request) {
        return ResponseEntity.ok(rendezVousService.updateStatus(id, request));
    }
}
