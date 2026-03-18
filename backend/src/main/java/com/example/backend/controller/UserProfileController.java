package com.example.backend.controller;

import com.example.backend.dto.UserProfileUpdateRequest;
import com.example.backend.service.UtilisateurService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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
}
