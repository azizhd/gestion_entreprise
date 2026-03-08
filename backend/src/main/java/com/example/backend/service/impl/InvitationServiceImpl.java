package com.example.backend.service.impl;

import com.example.backend.entitie.Invitation;
import com.example.backend.entitie.Utilisateur;
import com.example.backend.entitie.enumuration.TypeRole;
import com.example.backend.repository.InvitationRepository;
import com.example.backend.repository.UtilisateurRepository;
import com.example.backend.repository.EntrepriseRepository;
import com.example.backend.service.InvitationService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Transactional
public class InvitationServiceImpl implements InvitationService {

    @Autowired
    private InvitationRepository invitationRepository;

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    @Autowired
    private EntrepriseRepository entrepriseRepository;

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public Invitation createInvitation(Long inviterId, String invitedEmail) {
        // find inviter and entreprise
        Utilisateur inviter = utilisateurRepository.findById(inviterId)
                .orElseThrow(() -> new RuntimeException("Inviter not found"));

        Invitation inv = new Invitation();
        inv.setInvitedEmail(invitedEmail);
        inv.setInviterId(inviterId);
        inv.setEntreprise(inviter.getEntreprise());
        inv.setToken(UUID.randomUUID().toString());
        inv.setExpiresAt(LocalDateTime.now().plusDays(7));
        inv.setUsed(false);

        Invitation saved = invitationRepository.save(inv);

        // send email with token link (frontend should provide route to accept)
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(invitedEmail);
            helper.setSubject("Invitation to join " + inviter.getEntreprise().getNom());
            String body = "You have been invited to join " + inviter.getEntreprise().getNom() + ".\n" +
                    "Use this token to accept the invitation: " + saved.getToken();
            helper.setText(body, true);
            mailSender.send(message);
        } catch (MessagingException e) {
            // log and continue; invitation remains persisted
            System.err.println("Failed to send invitation email to " + invitedEmail + ": " + e.getMessage());
        }

        return saved;
    }

    @Override
    public void acceptInvitation(String token, String password, String nom, String prenom) {
        Invitation inv = invitationRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid invitation token"));

        if (inv.isUsed()) throw new RuntimeException("Invitation already used");
        if (inv.getExpiresAt().isBefore(LocalDateTime.now())) throw new RuntimeException("Invitation expired");

        // create user
        Utilisateur user = new Utilisateur();
        user.setEmail(inv.getInvitedEmail());
        user.setPassword(passwordEncoder.encode(password));
        user.setNom(nom);
        user.setPrenom(prenom);
        user.setRole(TypeRole.ROLE_EMPLOYEE);
        user.setEntreprise(inv.getEntreprise());

        utilisateurRepository.save(user);

        inv.setUsed(true);
        invitationRepository.save(inv);
    }
}
