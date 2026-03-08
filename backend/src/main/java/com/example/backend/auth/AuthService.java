package com.example.backend.auth;

import com.example.backend.dto.LoginRequest;
import com.example.backend.dto.LoginResponse;
import com.example.backend.dto.RegisterRequest;
import com.example.backend.dto.RegisterResponse;
import com.example.backend.dto.RefreshTokenRequest;
import com.example.backend.entitie.Utilisateur;
import com.example.backend.entitie.enumuration.TypeRole;
import com.example.backend.repository.UtilisateurRepository;
import com.example.backend.security.JwtTokenProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AuthService {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    @Autowired
    private com.example.backend.repository.EntrepriseRepository entrepriseRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider tokenProvider;

    // Register new user
    public RegisterResponse register(RegisterRequest request) {
        // Check if email already exists
        if (utilisateurRepository.findByEmail(request.getEmail()).isPresent()) {
            log.warn("Email already exists: {}", request.getEmail());
            return RegisterResponse.builder()
                .success(false)
                .message("Email already registered")
                .build();
        }

        // Validate password strength
        if (!isPasswordValid(request.getPassword())) {
            log.warn("Password does not meet security requirements for: {}", request.getEmail());
            return RegisterResponse.builder()
                .success(false)
                .message("Password must be at least 8 characters long")
                .build();
        }

        // First user gets ROLE_ADMIN, others get ROLE_ADMIN (manager by default as asked)
        TypeRole role = utilisateurRepository.count() == 0 ? TypeRole.ROLE_ADMIN : TypeRole.ROLE_ADMIN;

        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setNom(request.getNom());
        utilisateur.setPrenom(request.getPrenom());
        utilisateur.setEmail(request.getEmail());
        utilisateur.setPassword(passwordEncoder.encode(request.getPassword()));
        utilisateur.setTelephone(request.getTelephone());
        utilisateur.setRole(role);

        Utilisateur savedUser = utilisateurRepository.save(utilisateur);

        // Create Entreprise and link manager
        com.example.backend.entitie.Entreprise entreprise = new com.example.backend.entitie.Entreprise();
        entreprise.setNom(savedUser.getNom() + "'s Entreprise");
        entreprise.setEmail(savedUser.getEmail());
        entreprise.setTelephone(savedUser.getTelephone());
        entreprise.setManager(savedUser);
        entreprise = entrepriseRepository.save(entreprise);

        // Link back
        savedUser.setEntreprise(entreprise);
        utilisateurRepository.save(savedUser);

        log.info("User registered successfully: {} with role {} and entreprise id {}", savedUser.getEmail(), savedUser.getRole(), entreprise.getId());

        return RegisterResponse.builder()
            .success(true)
            .message("User registered successfully and entreprise created.")
            .userId(savedUser.getId())
            .email(savedUser.getEmail())
            .build();
    }

    // Login user
    public LoginResponse login(LoginRequest request) {
        try {
            // Authenticate user
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );

            // Get user details
            Utilisateur utilisateur = utilisateurRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Generate tokens
            String accessToken = tokenProvider.createAccessToken(utilisateur.getEmail(), utilisateur.getRole().toString());
            String refreshToken = tokenProvider.generateRefreshToken(utilisateur.getEmail());

            log.info("User logged in successfully: {}", utilisateur.getEmail());

            return LoginResponse.builder()
                    .token(accessToken)
                    .refreshToken(refreshToken)
                    .userId(utilisateur.getId())
                    .email(utilisateur.getEmail())
                    .nom(utilisateur.getNom())
                    .prenom(utilisateur.getPrenom())
                    .role(utilisateur.getRole().toString())
                    .entrepriseId(utilisateur.getEntreprise() != null ? utilisateur.getEntreprise().getId() : null)
                    .build();

        } catch (Exception e) {
            log.error("Login failed for email: {}", request.getEmail(), e);
            throw new RuntimeException("Invalid email or password");
        }
    }

    // Refresh token
    public LoginResponse refreshToken(RefreshTokenRequest request) {
        try {
            if (!tokenProvider.validateToken(request.getRefreshToken())) {
                throw new RuntimeException("Invalid refresh token");
            }

            String email = tokenProvider.getEmailFromToken(request.getRefreshToken());
            Utilisateur utilisateur = utilisateurRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            String newAccessToken = tokenProvider.createAccessToken(utilisateur.getEmail(), utilisateur.getRole().toString());

            log.info("Token refreshed for user: {}", email);

            return LoginResponse.builder()
                    .token(newAccessToken)
                    .refreshToken(request.getRefreshToken())
                    .userId(utilisateur.getId())
                    .email(utilisateur.getEmail())
                    .nom(utilisateur.getNom())
                    .prenom(utilisateur.getPrenom())
                    .role(utilisateur.getRole().toString())
                    .build();

        } catch (Exception e) {
            log.error("Token refresh failed", e);
            throw new RuntimeException("Could not refresh token");
        }
    }

    // Validate password strength
    private boolean isPasswordValid(String password) {
        return password != null && password.length() >= 8;
    }
}
