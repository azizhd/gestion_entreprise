package com.example.backend.controller;

import com.example.backend.dto.DevisCreateUpdateRequest;
import com.example.backend.dto.DevisDto;
import com.example.backend.dto.FactureDto;
import com.example.backend.service.DevisService;
import com.example.backend.service.PdfService;
import com.example.backend.service.NotificationService;
import com.example.backend.repository.DevisRepository;
import com.example.backend.entitie.Devis;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/devis")
@PreAuthorize("hasAnyRole('ADMIN','SECRETAIRE')")
@Slf4j
public class DevisController {

    private final DevisService devisService;
    private final PdfService pdfService;
    private final NotificationService notificationService;
    private final DevisRepository devisRepository;

    public DevisController(DevisService devisService, PdfService pdfService, NotificationService notificationService, DevisRepository devisRepository) {
        this.devisService = devisService;
        this.pdfService = pdfService;
        this.notificationService = notificationService;
        this.devisRepository = devisRepository;
    }

    @GetMapping
    public ResponseEntity<Page<DevisDto>> listAll(@RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(devisService.getAllDevis(page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DevisDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(devisService.getDevisById(id));
    }

    @PostMapping
    public ResponseEntity<DevisDto> create(@Valid @RequestBody DevisCreateUpdateRequest devisDto) {
        DevisDto created = devisService.createDevis(devisDto);
        return ResponseEntity.ok(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<DevisDto> update(@PathVariable Long id, @Valid @RequestBody DevisCreateUpdateRequest devisDto) {
        DevisDto updated = devisService.updateDevis(id, devisDto);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        devisService.deleteDevis(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/transformer-en-facture")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<FactureDto> transformer(@PathVariable Long id) {
        return ResponseEntity.ok(devisService.transformerEnFacture(id));
    }

    @GetMapping("/{id}/pdf")
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> getPdf(@PathVariable Long id) {
        Devis devis = devisRepository.findById(id).orElse(null);
        if (devis == null) {
            return ResponseEntity.notFound().build();
        }
        try {
            Hibernate.initialize(devis.getLignesdevis());
            if (devis.getClient() != null) {
                Hibernate.initialize(devis.getClient());
                if (devis.getClient().getEntreprise() != null) {
                    Hibernate.initialize(devis.getClient().getEntreprise());
                }
            }
            byte[] pdf = pdfService.generateDevisPdf(devis);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "devis-" + (devis.getReference() == null ? id.toString() : devis.getReference()) + ".pdf");
            return ResponseEntity.ok().headers(headers).body(pdf);
        } catch (Exception e) {
            log.error("Failed to generate PDF for devis {}", id, e);
            HttpHeaders errorHeaders = new HttpHeaders();
            errorHeaders.add("X-Error-Message", "PDF_GENERATION_FAILED");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .headers(errorHeaders)
                    .body(null);
        }
    }

    @PostMapping("/{id}/send")
    public ResponseEntity<Void> sendToClient(@PathVariable Long id) {
        Devis devis = devisRepository.findById(id).orElse(null);
        if (devis == null) return ResponseEntity.notFound().build();
        if (devis.getClient() == null || devis.getClient().getEmail() == null) return ResponseEntity.badRequest().build();
        try {
            byte[] pdf = pdfService.generateDevisPdf(devis);
            String to = devis.getClient().getEmail();
            String subject = "Votre devis " + (devis.getReference() == null ? "" : devis.getReference());
            String body = "Bonjour,\n\nVeuillez trouver en pièce jointe votre devis.\n\nCordialement.";
            notificationService.sendEmailWithAttachment(to, subject, body, pdf, "devis-" + (devis.getReference() == null ? id.toString() : devis.getReference()) + ".pdf");
            return ResponseEntity.accepted().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
