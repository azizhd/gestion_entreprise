package com.example.backend.ai.security;

import com.example.backend.entitie.Utilisateur;
import com.example.backend.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class AiRequestContext {

    private final UtilisateurRepository utilisateurRepository;

    public Optional<String> currentRole() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getAuthorities() == null || authentication.getAuthorities().isEmpty()) {
            return Optional.empty();
        }
        return authentication.getAuthorities().stream().findFirst().map(Object::toString);
    }

    public Optional<String> currentEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(authentication.getName());
    }

    public Optional<Long> currentEnterpriseId() {
        return currentEmail()
                .flatMap(utilisateurRepository::findByEmail)
                .map(Utilisateur::getEntreprise)
                .map(ent -> ent != null ? Long.valueOf(ent.getId()) : null);
    }
}
