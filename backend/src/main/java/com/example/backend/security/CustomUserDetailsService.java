package com.example.backend.security;

import com.example.backend.entitie.Utilisateur;
import com.example.backend.repository.EntrepriseRepository;
import com.example.backend.repository.UtilisateurRepository;
import java.util.Collections;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UtilisateurRepository utilisateurRepository;
    private final EntrepriseRepository entrepriseRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Utilisateur utilisateur = utilisateurRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        if (utilisateur.getEntreprise() != null) {
            var entreprise = entrepriseRepository.findById(utilisateur.getEntreprise().getId()).orElse(null);
            if (entreprise != null && Boolean.TRUE.equals(entreprise.getDeleted())) {
                throw new UsernameNotFoundException("Entreprise inactive");
            }
        }

        return new CustomUserPrincipal(
            utilisateur.getId(),
            utilisateur.getEmail(),
            utilisateur.getPassword(),
            utilisateur.getRole(),
            Collections.singletonList(new SimpleGrantedAuthority(utilisateur.getRole().toString()))
        );
    }
}
