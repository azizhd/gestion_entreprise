package com.example.backend.controller;

import com.example.backend.dto.UserPhotoDownload;
import com.example.backend.service.UtilisateurService;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserPhotoController {

    private final UtilisateurService utilisateurService;

    public UserPhotoController(UtilisateurService utilisateurService) {
        this.utilisateurService = utilisateurService;
    }

    @GetMapping("/{id}/photo")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Resource> downloadPhoto(@PathVariable Long id) {
        try {
            UserPhotoDownload download = utilisateurService.getUserPhoto(id);
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(download.contentType()))
                    .body(download.resource());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (AccessDeniedException | SecurityException e) {
            return ResponseEntity.status(403).build();
        }
    }
}
