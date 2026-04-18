package com.example.backend.controller;

import com.example.backend.dto.UserProfileUpdateRequest;
import com.example.backend.service.UtilisateurService;
import jakarta.validation.Valid;
import com.example.backend.dto.UserPhotoDownload;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/users/me")
public class UserProfileController {

    private final UtilisateurService utilisateurService;

    public UserProfileController(UtilisateurService utilisateurService) {
        this.utilisateurService = utilisateurService;
    }

    @PutMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> updateProfile(@Valid @RequestBody UserProfileUpdateRequest request) {
        try {
            return ResponseEntity.ok(utilisateurService.updateCurrentUser(request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(e.getMessage());
        }
    }

    @PostMapping(value = "/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> uploadPhoto(@RequestPart("file") MultipartFile file) {
        try {
            return ResponseEntity.ok(utilisateurService.updateCurrentUserPhoto(file));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(e.getMessage());
        }
    }

    @GetMapping("/photo")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Resource> downloadPhoto() {
        try {
            UserPhotoDownload download = utilisateurService.getCurrentUserPhoto();
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(download.contentType()))
                    .body(download.resource());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (SecurityException e) {
            return ResponseEntity.status(403).build();
        }
    }

    @DeleteMapping("/photo")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> removePhoto() {
        try {
            return ResponseEntity.ok(utilisateurService.removeCurrentUserPhoto());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(e.getMessage());
        }
    }
}
