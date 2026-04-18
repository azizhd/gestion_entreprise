package com.example.backend.controller;

import com.example.backend.dto.EntrepriseDto;
import com.example.backend.dto.EntrepriseDeleteRequest;
import com.example.backend.dto.EntrepriseLogoDownload;
import com.example.backend.service.EntrepriseService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/entreprises")
public class EntrepriseController {

    private final EntrepriseService entrepriseService;

    public EntrepriseController(EntrepriseService entrepriseService) {
        this.entrepriseService = entrepriseService;
    }

    @GetMapping
    public ResponseEntity<?> getAll() {
        try {
            return ResponseEntity.ok(entrepriseService.getAll());
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Integer id) {
        try {
            EntrepriseDto dto = entrepriseService.getById(id);
            if (dto == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(dto);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody EntrepriseDto dto) {
        try {
            EntrepriseDto created = entrepriseService.create(dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Integer id, @RequestBody EntrepriseDto dto) {
        try {
            EntrepriseDto updated = entrepriseService.update(id, dto);
            if (updated == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        try {
            entrepriseService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    @PostMapping("/{id}/delete-request")
    public ResponseEntity<?> requestDelete(@PathVariable Integer id, @RequestBody EntrepriseDeleteRequest request) {
        try {
            entrepriseService.requestDeletion(id, request);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }

    @PostMapping(value = "/{id}/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> uploadLogo(@PathVariable Integer id, @RequestPart("file") MultipartFile file) {
        try {
            return ResponseEntity.ok(entrepriseService.updateLogo(id, file));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }

    @GetMapping("/{id}/logo")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Resource> downloadLogo(@PathVariable Integer id) {
        try {
            EntrepriseLogoDownload download = entrepriseService.getLogo(id);
            String contentType = download.contentType();
            MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
            if (contentType != null && !contentType.isBlank()) {
                mediaType = MediaType.parseMediaType(contentType);
            }
            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .body(download.resource());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    @DeleteMapping("/{id}/logo")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> removeLogo(@PathVariable Integer id) {
        try {
            return ResponseEntity.ok(entrepriseService.removeLogo(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }
}
