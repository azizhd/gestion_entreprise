package com.example.backend.service.impl;

import com.example.backend.audit.AuditAction;
import com.example.backend.audit.ActionType;
import com.example.backend.dto.UserPhotoDownload;
import com.example.backend.dto.UtilisateurDTO;
import com.example.backend.dto.UserProfileUpdateRequest;
import com.example.backend.entitie.Entreprise;
import com.example.backend.entitie.Utilisateur;
import com.example.backend.entitie.enumuration.TypeRole;
import com.example.backend.mapper.UtilisateurMapper;
import com.example.backend.repository.EntrepriseRepository;
import com.example.backend.repository.UtilisateurRepository;
import com.example.backend.service.UtilisateurService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class UtilisateurServiceImpl implements UtilisateurService {

    private static final Set<String> ALLOWED_PHOTO_EXTENSIONS = Set.of("png", "jpg", "jpeg");

    private final UtilisateurRepository utilisateurRepository;
    private final UtilisateurMapper utilisateurMapper;
    private final PasswordEncoder passwordEncoder;
    private final EntrepriseRepository entrepriseRepository;
    private final Path photoStorageRoot;

    public UtilisateurServiceImpl(UtilisateurRepository utilisateurRepository,
                                  UtilisateurMapper utilisateurMapper,
                                  PasswordEncoder passwordEncoder,
                                  EntrepriseRepository entrepriseRepository,
                                  @Value("${app.users.photos.storage:uploads/photos}") String photoStoragePath) {
        this.utilisateurRepository = utilisateurRepository;
        this.utilisateurMapper = utilisateurMapper;
        this.passwordEncoder = passwordEncoder;
        this.entrepriseRepository = entrepriseRepository;
        this.photoStorageRoot = Paths.get(photoStoragePath).toAbsolutePath().normalize();
    }

    @Override
    @AuditAction(action = "Assign Role", entityType = "Utilisateur", type = ActionType.UPDATE)
    @PreAuthorize("hasRole('ADMIN')")
    public UtilisateurDTO assignRole(Long userId, TypeRole newRole) {
        Utilisateur current = resolveCurrentUser();
        Entreprise entreprise = resolveCurrentEntreprise(current);
        Utilisateur utilisateur = utilisateurRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (!isSameEntreprise(entreprise, utilisateur)) {
            throw new SecurityException("Access denied to this user");
        }
        utilisateur.setRole(newRole);
        utilisateurRepository.save(utilisateur);
        return utilisateurMapper.toDto(utilisateur);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public List<UtilisateurDTO> getAllUsers() {
        Utilisateur current = resolveCurrentUser();
        Entreprise entreprise = resolveCurrentEntreprise(current);
        if (entreprise == null) {
            return List.of();
        }
        return utilisateurRepository.findByEntreprise_Id(entreprise.getId()).stream()
                .map(utilisateurMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public UtilisateurDTO getUserById(Long id) {
        Utilisateur current = resolveCurrentUser();
        Entreprise entreprise = resolveCurrentEntreprise(current);
        Utilisateur utilisateur = utilisateurRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (!isSameEntreprise(entreprise, utilisateur)) {
            throw new SecurityException("Access denied to this user");
        }
        return utilisateurMapper.toDto(utilisateur);
    }

    @Override
    @AuditAction(action = "Update User", entityType = "Utilisateur", type = ActionType.UPDATE)
    @PreAuthorize("hasRole('ADMIN')")
    public UtilisateurDTO updateUser(Long id, UtilisateurDTO utilisateurDTO) {
        Utilisateur current = resolveCurrentUser();
        Entreprise entreprise = resolveCurrentEntreprise(current);
        Utilisateur utilisateur = utilisateurRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (!isSameEntreprise(entreprise, utilisateur)) {
            throw new SecurityException("Access denied to this user");
        }
        utilisateur.setNom(utilisateurDTO.getNom());
        utilisateur.setPrenom(utilisateurDTO.getPrenom());
        utilisateur.setEmail(utilisateurDTO.getEmail());
        utilisateur.setTelephone(utilisateurDTO.getTelephone());
        utilisateur.setPhoto(utilisateurDTO.getPhoto());
        utilisateurRepository.save(utilisateur);
        return utilisateurMapper.toDto(utilisateur);
    }

    @Override
    @AuditAction(action = "Update Current User", entityType = "Utilisateur", type = ActionType.UPDATE)
    @PreAuthorize("isAuthenticated()")
    public UtilisateurDTO updateCurrentUser(UserProfileUpdateRequest request) {
        Utilisateur current = resolveCurrentUser();

        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            String nextEmail = request.getEmail().trim();
            if (!nextEmail.equalsIgnoreCase(current.getEmail())) {
                utilisateurRepository.findByEmail(nextEmail)
                        .filter(existing -> !existing.getId().equals(current.getId()))
                        .ifPresent(existing -> { throw new IllegalArgumentException("Email already in use"); });
                current.setEmail(nextEmail);
            }
        }

        if (request.getNom() != null) {
            current.setNom(request.getNom());
        }
        if (request.getPrenom() != null) {
            current.setPrenom(request.getPrenom());
        }

        if (request.getNewPassword() != null && !request.getNewPassword().isBlank()) {
            String currentPassword = request.getCurrentPassword();
            if (currentPassword == null || currentPassword.isBlank()) {
                throw new IllegalArgumentException("Current password is required");
            }
            if (!passwordEncoder.matches(currentPassword, current.getPassword())) {
                throw new IllegalArgumentException("Invalid current password");
            }
            if (request.getNewPassword().length() < 8) {
                throw new IllegalArgumentException("Password must be at least 8 characters");
            }
            current.setPassword(passwordEncoder.encode(request.getNewPassword()));
        }

        utilisateurRepository.save(current);
        return utilisateurMapper.toDto(current);
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public UtilisateurDTO updateCurrentUserPhoto(MultipartFile file) {
        Utilisateur current = resolveCurrentUser();
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Fichier requis");
        }
        String originalName = sanitizeFilename(file.getOriginalFilename());
        String extension = getExtension(originalName);
        if (!ALLOWED_PHOTO_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("Type de fichier non autorise");
        }
        String storedName = UUID.randomUUID() + "_" + originalName;
        Path target = photoStorageRoot.resolve(storedName).normalize();
        try {
            Files.createDirectories(photoStorageRoot);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new IllegalStateException("Impossible de sauvegarder la photo", ex);
        }

        deleteExistingPhoto(current.getPhoto());
        current.setPhoto(storedName);
        utilisateurRepository.save(current);
        return utilisateurMapper.toDto(current);
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public UtilisateurDTO removeCurrentUserPhoto() {
        Utilisateur current = resolveCurrentUser();
        deleteExistingPhoto(current.getPhoto());
        current.setPhoto(null);
        utilisateurRepository.save(current);
        return utilisateurMapper.toDto(current);
    }

    @Override
    @Transactional(readOnly = true)
    public UserPhotoDownload getCurrentUserPhoto() {
        Utilisateur current = resolveCurrentUser();
        return buildPhotoDownload(current);
    }

    @Override
    @Transactional(readOnly = true)
    public UserPhotoDownload getUserPhoto(Long userId) {
        Utilisateur current = resolveCurrentUser();
        Utilisateur target = utilisateurRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (!canViewPhoto(current, target)) {
            throw new AccessDeniedException("Acces refuse");
        }
        return buildPhotoDownload(target);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public void deactivateUser(Long id) {
        Utilisateur current = resolveCurrentUser();
        Entreprise entreprise = resolveCurrentEntreprise(current);
        Utilisateur utilisateur = utilisateurRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (!isSameEntreprise(entreprise, utilisateur)) {
            throw new SecurityException("Access denied to this user");
        }
        utilisateur.setRole(null); // Or add an 'active' flag if you prefer
        utilisateurRepository.save(utilisateur);
    }

    @Override
    @AuditAction(action = "Reactivate User", entityType = "Utilisateur", type = ActionType.UPDATE)
    @PreAuthorize("hasRole('ADMIN')")
    public void reactivateUser(Long id) {
        Utilisateur current = resolveCurrentUser();
        Entreprise entreprise = resolveCurrentEntreprise(current);
        Utilisateur utilisateur = utilisateurRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (!isSameEntreprise(entreprise, utilisateur)) {
            throw new SecurityException("Access denied to this user");
        }
        if (utilisateur.getRole() == null) {
            utilisateur.setRole(TypeRole.ROLE_EMPLOYEE); // Default role, or restore previous
            utilisateurRepository.save(utilisateur);
        }
    }

    @Override
    @AuditAction(action = "Create User", entityType = "Utilisateur", type = ActionType.CREATE)
    @PreAuthorize("hasRole('ADMIN')")
    public UtilisateurDTO createUser(UtilisateurDTO utilisateurDTO, String password) {
        Utilisateur current = resolveCurrentUser();
        Entreprise entreprise = resolveCurrentEntreprise(current);
        if (entreprise == null) {
            throw new IllegalArgumentException("Current admin is not linked to an entreprise");
        }

        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setNom(utilisateurDTO.getNom());
        utilisateur.setPrenom(utilisateurDTO.getPrenom());
        utilisateur.setEmail(utilisateurDTO.getEmail());
        utilisateur.setTelephone(utilisateurDTO.getTelephone());
        utilisateur.setPhoto(utilisateurDTO.getPhoto());
        utilisateur.setRole(utilisateurDTO.getRole() == null ? TypeRole.ROLE_EMPLOYEE : utilisateurDTO.getRole());

        if (password != null && !password.isBlank()) {
            utilisateur.setPassword(passwordEncoder.encode(password));
        } else {
            utilisateur.setPassword(passwordEncoder.encode("changeme"));
        }

        utilisateur.setEntreprise(entreprise);

        utilisateurRepository.save(utilisateur);
        return utilisateurMapper.toDto(utilisateur);
    }

    @Override
    @AuditAction(action = "Delete User", entityType = "Utilisateur", type = ActionType.DELETE)
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteUser(Long id) {
        Utilisateur current = resolveCurrentUser();
        Entreprise entreprise = resolveCurrentEntreprise(current);
        Utilisateur utilisateur = utilisateurRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (!isSameEntreprise(entreprise, utilisateur)) {
            throw new SecurityException("Access denied to this user");
        }
        utilisateurRepository.delete(utilisateur);
    }

    private Utilisateur resolveCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new SecurityException("No authenticated user");
        }
        return utilisateurRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new SecurityException("Authenticated user not found"));
    }

    private Entreprise resolveCurrentEntreprise(Utilisateur currentUser) {
        return entrepriseRepository.findByManager_IdAndDeletedFalse(currentUser.getId())
                .orElseGet(() -> {
                    if (currentUser.getEntreprise() == null) {
                        return null;
                    }
                    return entrepriseRepository.findById(currentUser.getEntreprise().getId()).orElse(null);
                });
    }

    private boolean isSameEntreprise(Entreprise entreprise, Utilisateur target) {
        if (entreprise == null || target == null) {
            return false;
        }
        if (target.getEntreprise() != null && target.getEntreprise().getId() == entreprise.getId()) {
            return true;
        }
        return entreprise.getManager() != null && entreprise.getManager().getId().equals(target.getId());
    }

    private boolean canViewPhoto(Utilisateur current, Utilisateur target) {
        if (current == null || target == null) {
            return false;
        }
        if (current.getId().equals(target.getId())) {
            return true;
        }
        Entreprise entreprise = resolveCurrentEntreprise(current);
        return isSameEntreprise(entreprise, target);
    }

    private UserPhotoDownload buildPhotoDownload(Utilisateur utilisateur) {
        if (utilisateur.getPhoto() == null || utilisateur.getPhoto().isBlank()) {
            throw new IllegalArgumentException("Photo introuvable");
        }
        Path filePath = photoStorageRoot.resolve(utilisateur.getPhoto()).normalize();
        try {
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists()) {
                throw new IllegalArgumentException("Photo introuvable");
            }
            String contentType = resolveContentType(filePath);
            return new UserPhotoDownload(resource, utilisateur.getPhoto(), contentType);
        } catch (IOException ex) {
            throw new IllegalStateException("Impossible de lire la photo", ex);
        }
    }

    private void deleteExistingPhoto(String photoName) {
        if (photoName == null || photoName.isBlank()) {
            return;
        }
        Path existingPath = photoStorageRoot.resolve(photoName).normalize();
        try {
            Files.deleteIfExists(existingPath);
        } catch (IOException ex) {
            throw new IllegalStateException("Impossible de supprimer l'ancienne photo", ex);
        }
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

    private String sanitizeFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return "photo";
        }
        return filename.replaceAll("[\\\\/\n\r\t]", "_").trim();
    }
}
