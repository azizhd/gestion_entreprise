package com.example.backend.service.impl;

import com.example.backend.audit.AuditAction;
import com.example.backend.audit.ActionType;
import com.example.backend.dto.EntrepriseDto;
import com.example.backend.dto.EntrepriseDeleteRequest;
import com.example.backend.dto.EntrepriseLogoDownload;
import com.example.backend.entitie.Abonnement;
import com.example.backend.entitie.Entreprise;
import com.example.backend.entitie.Utilisateur;
import com.example.backend.entitie.enumuration.AbonnementType;
import com.example.backend.mapper.EntrepriseMapper;
import com.example.backend.repository.EntrepriseRepository;
import com.example.backend.repository.UtilisateurRepository;
import com.example.backend.service.EntrepriseService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.time.LocalDateTime;
import java.util.Locale;

@Service
@Transactional
public class EntrepriseServiceImpl implements EntrepriseService {

    private static final Set<String> ALLOWED_LOGO_EXTENSIONS = Set.of("png", "jpg", "jpeg");

    private final EntrepriseRepository entrepriseRepository;
    private final EntrepriseMapper entrepriseMapper;
    private final UtilisateurRepository utilisateurRepository;
    private final AuthenticationManager authenticationManager;
    private final Path logoStorageRoot;

    public EntrepriseServiceImpl(EntrepriseRepository entrepriseRepository,
                                 EntrepriseMapper entrepriseMapper,
                                 UtilisateurRepository utilisateurRepository,
                                 AuthenticationManager authenticationManager,
                                 @Value("${app.entreprises.logos.storage:uploads/logos}") String logoStoragePath) {
        this.entrepriseRepository = entrepriseRepository;
        this.entrepriseMapper = entrepriseMapper;
        this.utilisateurRepository = utilisateurRepository;
        this.authenticationManager = authenticationManager;
        this.logoStorageRoot = Paths.get(logoStoragePath).toAbsolutePath().normalize();
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public List<EntrepriseDto> getAll() {
        Utilisateur currentUser = resolveCurrentUser();
        Entreprise entreprise = findEntrepriseForUser(currentUser);
        if (entreprise == null || Boolean.TRUE.equals(entreprise.getDeleted())) {
            return List.of();
        }
        return List.of(entrepriseMapper.toDTO(entreprise));
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public EntrepriseDto getById(Integer id) {
        Utilisateur currentUser = resolveCurrentUser();
        Entreprise entreprise = entrepriseRepository.findById(id).orElse(null);
        if (entreprise == null || Boolean.TRUE.equals(entreprise.getDeleted())) {
            return null;
        }
        if (!isUserRelatedToEntreprise(currentUser, entreprise)) {
            throw new SecurityException("Access denied to this entreprise");
        }
        return entrepriseMapper.toDTO(entreprise);
    }

    @Override
    @AuditAction(action = "ENTREPRISE_CREATE", entityType = "Entreprise", type = ActionType.CREATE)
    @PreAuthorize("hasRole('ADMIN')")
    public EntrepriseDto create(EntrepriseDto dto) {
        Entreprise entreprise = new Entreprise();
        applyDtoToEntity(dto, entreprise);
        Entreprise saved = entrepriseRepository.save(entreprise);
        return entrepriseMapper.toDTO(saved);
    }

    @Override
    @AuditAction(action = "ENTREPRISE_UPDATE", entityType = "Entreprise", type = ActionType.UPDATE)
    @PreAuthorize("hasRole('ADMIN')")
    public EntrepriseDto update(Integer id, EntrepriseDto dto) {
        Utilisateur currentUser = resolveCurrentUser();
        Optional<Entreprise> existingOpt = entrepriseRepository.findById(id);
        if (existingOpt.isEmpty()) {
            return null;
        }
        Entreprise existing = existingOpt.get();
        if (Boolean.TRUE.equals(existing.getDeleted())) {
            throw new IllegalArgumentException("Entreprise inactive");
        }
        if (existing.getManager() == null || !existing.getManager().getId().equals(currentUser.getId())) {
            throw new SecurityException("Only the manager can update this entreprise");
        }
        applyDtoToEntity(dto, existing);
        Entreprise saved = entrepriseRepository.save(existing);
        return entrepriseMapper.toDTO(saved);
    }

    @Override
    @AuditAction(action = "ENTREPRISE_DELETE", entityType = "Entreprise", type = ActionType.DELETE)
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(Integer id) {
        Utilisateur currentUser = resolveCurrentUser();
        Entreprise existing = entrepriseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Entreprise not found"));
        if (Boolean.TRUE.equals(existing.getDeleted())) {
            return;
        }
        if (existing.getManager() == null || !existing.getManager().getId().equals(currentUser.getId())) {
            throw new SecurityException("Only the manager can delete this entreprise");
        }
        entrepriseRepository.delete(existing);
    }

    @Override
    @AuditAction(action = "ENTREPRISE_DELETE_REQUEST", entityType = "Entreprise", type = ActionType.DELETE)
    @PreAuthorize("hasRole('ADMIN')")
    public void requestDeletion(Integer id, EntrepriseDeleteRequest request) {
        Utilisateur currentUser = resolveCurrentUser();
        Entreprise entreprise = entrepriseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Entreprise not found"));
        if (Boolean.TRUE.equals(entreprise.getDeleted())) {
            return;
        }
        if (entreprise.getManager() == null || !entreprise.getManager().getId().equals(currentUser.getId())) {
            throw new SecurityException("Only the manager can delete this entreprise");
        }
        if (request == null || request.password() == null || request.password().isBlank()) {
            throw new IllegalArgumentException("Password required");
        }
        if (!Boolean.TRUE.equals(request.acknowledge())) {
            throw new IllegalArgumentException("Acknowledgement required");
        }
        String expectedName = entreprise.getNom() != null ? entreprise.getNom().trim() : "";
        String confirmName = request.confirmName() != null ? request.confirmName().trim() : "";
        if (!expectedName.equals(confirmName)) {
            throw new IllegalArgumentException("Confirmation name does not match");
        }

        authenticateForDeletion(currentUser.getEmail(), request.password());

        entreprise.setDeleted(Boolean.TRUE);
        entreprise.setDeletionRequestedAt(LocalDateTime.now());
        entreprise.setPurgeAt(LocalDateTime.now().plusDays(30));
        entrepriseRepository.save(entreprise);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public EntrepriseDto updateLogo(Integer id, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Fichier logo requis");
        }
        Utilisateur currentUser = resolveCurrentUser();
        Entreprise entreprise = entrepriseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Entreprise not found"));
        if (entreprise.getManager() == null || !entreprise.getManager().getId().equals(currentUser.getId())) {
            throw new SecurityException("Only the manager can update this entreprise");
        }

        String extension = getExtension(file.getOriginalFilename());
        if (!ALLOWED_LOGO_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("Format de logo non supporte. Utilisez PNG ou JPG.");
        }

        String storedName = "entreprise-" + entreprise.getId() + "-" + UUID.randomUUID() + "." + extension;
        Path target = logoStorageRoot.resolve(storedName).normalize();
        try {
            Files.createDirectories(logoStorageRoot);
            file.transferTo(target);
        } catch (IOException ex) {
            throw new IllegalStateException("Impossible de sauvegarder le logo", ex);
        }

        deleteExistingLogo(entreprise.getLogo());
        entreprise.setLogo(storedName);
        Entreprise saved = entrepriseRepository.save(entreprise);
        return entrepriseMapper.toDTO(saved);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public EntrepriseDto removeLogo(Integer id) {
        Utilisateur currentUser = resolveCurrentUser();
        Entreprise entreprise = entrepriseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Entreprise not found"));
        if (entreprise.getManager() == null || !entreprise.getManager().getId().equals(currentUser.getId())) {
            throw new SecurityException("Only the manager can update this entreprise");
        }
        deleteExistingLogo(entreprise.getLogo());
        entreprise.setLogo(null);
        Entreprise saved = entrepriseRepository.save(entreprise);
        return entrepriseMapper.toDTO(saved);
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public EntrepriseLogoDownload getLogo(Integer id) {
        Utilisateur currentUser = resolveCurrentUser();
        Entreprise entreprise = entrepriseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Entreprise not found"));
        if (!isUserRelatedToEntreprise(currentUser, entreprise)) {
            throw new SecurityException("Access denied to this entreprise");
        }
        if (entreprise.getLogo() == null || entreprise.getLogo().isBlank()) {
            throw new IllegalArgumentException("Logo introuvable");
        }

        Path filePath = resolveLogoPath(entreprise.getLogo());
        try {
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists()) {
                throw new IllegalArgumentException("Logo introuvable");
            }
            String contentType = resolveContentType(filePath);
            return new EntrepriseLogoDownload(resource, entreprise.getLogo(), contentType);
        } catch (IOException ex) {
            throw new IllegalStateException("Impossible de lire le logo", ex);
        }
    }

    private void applyDtoToEntity(EntrepriseDto dto, Entreprise entity) {
        entity.setNom(dto.getNom());
        if (dto.getLogo() != null) {
            entity.setLogo(dto.getLogo());
        }
        entity.setEmail(dto.getEmail());
        entity.setTelephone(dto.getTelephone());
        entity.setLocation(dto.getLocation());

        Utilisateur manager = resolveCurrentUser();

        entrepriseRepository.findByManager_IdAndDeletedFalse(manager.getId())
                .filter(existing -> entity.getId() == 0 || existing.getId() != entity.getId())
                .ifPresent(existing -> {
                    AbonnementType tier = existing.getAbonnement() != null ? existing.getAbonnement().getAbonnementType() : null;
                    if (tier == null || (tier != AbonnementType.AVANCE && tier != AbonnementType.PREMIUM)) {
                        throw new IllegalArgumentException("Manager already manages an entreprise; requires AVANCE or PREMIUM abonnement on the existing entreprise to manage multiple.");
                    }
                });

        if (entity.getAbonnement() == null) {
            Abonnement abonnement = new Abonnement();
            abonnement.setAbonnementType(AbonnementType.FREE_TRIAL_15DAYS);
            abonnement.setDateDebut(LocalDateTime.now());
            abonnement.setDateFin(LocalDateTime.now().plusDays(15));
            abonnement.setStatus(Boolean.TRUE);
            entity.setAbonnement(abonnement);
        }

        entity.setManager(manager);
    }

    private Utilisateur resolveCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new IllegalArgumentException("No authenticated user");
        }
        return utilisateurRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new IllegalArgumentException("Authenticated user not found"));
    }

    private void authenticateForDeletion(String email, String password) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, password));
    }

    private Entreprise findEntrepriseForUser(Utilisateur user) {
        return entrepriseRepository.findByManager_IdAndDeletedFalse(user.getId())
                .orElseGet(() -> {
                    if (user.getEntreprise() == null) {
                        return null;
                    }
                    return entrepriseRepository.findById(user.getEntreprise().getId()).orElse(null);
                });
    }

    private boolean isUserRelatedToEntreprise(Utilisateur user, Entreprise entreprise) {
        if (entreprise.getManager() != null && entreprise.getManager().getId() != null
                && entreprise.getManager().getId().equals(user.getId())) {
            return true;
        }
        return user.getEntreprise() != null && entreprise.getId() == user.getEntreprise().getId();
    }

    private void deleteExistingLogo(String logoName) {
        if (logoName == null || logoName.isBlank()) {
            return;
        }
        Path existingPath = resolveLogoPath(logoName);
        try {
            Files.deleteIfExists(existingPath);
        } catch (IOException ex) {
            throw new IllegalStateException("Impossible de supprimer l'ancien logo", ex);
        }
    }

    private Path resolveLogoPath(String logoName) {
        Path rawPath = Paths.get(logoName);
        if (rawPath.isAbsolute()) {
            return rawPath.normalize();
        }
        return logoStorageRoot.resolve(logoName).normalize();
    }

    private String resolveContentType(Path path) {
        try {
            String contentType = Files.probeContentType(path);
            return contentType != null ? contentType : "application/octet-stream";
        } catch (IOException ex) {
            return "application/octet-stream";
        }
    }

    private String getExtension(String filename) {
        if (filename == null) return "";
        int idx = filename.lastIndexOf('.');
        if (idx < 0) return "";
        return filename.substring(idx + 1).toLowerCase(Locale.ROOT);
    }
}
