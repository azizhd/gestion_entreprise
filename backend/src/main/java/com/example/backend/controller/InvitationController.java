package com.example.backend.controller;

import com.example.backend.entitie.Invitation;
import com.example.backend.service.InvitationService;
import com.example.backend.repository.UtilisateurRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/invitations")
public class InvitationController {

    @Autowired
    private InvitationService invitationService;

    public static class CreateInviteRequest {
        public String invitedEmail;
    }

    public static class AcceptInviteRequest {
        public String token;
        public String password;
        public String nom;
        public String prenom;
    }

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Invitation> createInvite(@RequestBody CreateInviteRequest req) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Long inviterId = utilisateurRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Inviter not found"))
                .getId();
        Invitation invitation = invitationService.createInvitation(inviterId, req.invitedEmail);
        return ResponseEntity.ok(invitation);
    }

    @PostMapping("/accept")
    public ResponseEntity<Void> acceptInvite(@RequestBody AcceptInviteRequest req) {
        invitationService.acceptInvitation(req.token, req.password, req.nom, req.prenom);
        return ResponseEntity.ok().build();
    }
}
