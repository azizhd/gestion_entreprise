package com.example.backend.service.impl;

import com.example.backend.dto.DocumentDownload;
import com.example.backend.dto.DocumentDto;
import com.example.backend.entitie.Document;
import com.example.backend.entitie.Entreprise;
import com.example.backend.entitie.Utilisateur;
import com.example.backend.entitie.enumuration.TypeRole;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.mapper.DocumentMapper;
import com.example.backend.repository.DocumentRepository;
import com.example.backend.repository.EntrepriseRepository;
import com.example.backend.repository.UtilisateurRepository;
import com.example.backend.service.DocumentService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional
public class DocumentServiceImpl implements DocumentService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "docx", "xlsx", "png", "jpg", "jpeg");

    private final DocumentRepository documentRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final EntrepriseRepository entrepriseRepository;
    private final DocumentMapper documentMapper;
    private final Path storageRoot;

    public DocumentServiceImpl(DocumentRepository documentRepository,
                               UtilisateurRepository utilisateurRepository,
                               EntrepriseRepository entrepriseRepository,
                               DocumentMapper documentMapper,
                               @Value("${app.documents.storage:uploads/documents}") String storagePath) {
        this.documentRepository = documentRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.entrepriseRepository = entrepriseRepository;
        this.documentMapper = documentMapper;
        this.storageRoot = Paths.get(storagePath).toAbsolutePath().normalize();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DocumentDto> list(String type,
                                  String category,
                                  String search,
                                  LocalDateTime fromDate,
                                  LocalDateTime toDate,
                                  Pageable pageable) {
        Utilisateur current = resolveCurrentUser();
        Entreprise entreprise = resolveCurrentEntreprise(current);
        Long uploadedById = isAdmin(current) ? null : current.getId();
        if (!isAdmin(current) && !isSecretary(current) && !isAccountant(current)) {
            throw new AccessDeniedException("Acces refuse");
        }
        Page<Document> page = documentRepository.search(
                entreprise.getId(),
                uploadedById,
                blankToNull(type),
                blankToNull(category),
                fromDate,
                toDate,
                blankToNull(search),
                pageable
        );
        return page.map(documentMapper::toDto);
    }

    @Override
    public DocumentDto upload(MultipartFile file, String title, String category, String description) {
        Utilisateur current = resolveCurrentUser();
        if (!isAdmin(current) && !isSecretary(current) && !isAccountant(current)) {
            throw new AccessDeniedException("Acces refuse");
        }
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Fichier requis");
        }
        String originalName = sanitizeFilename(file.getOriginalFilename());
        String extension = getExtension(originalName);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("Type de fichier non autorise");
        }
        Entreprise entreprise = resolveCurrentEntreprise(current);
        String storedName = UUID.randomUUID() + "_" + originalName;
        Path target = storageRoot.resolve(storedName).normalize();
        try {
            Files.createDirectories(storageRoot);
            Files.copy(file.getInputStream(), target);
        } catch (IOException ex) {
            throw new IllegalStateException("Impossible de sauvegarder le fichier", ex);
        }

        Document document = new Document();
        document.setTitle(title != null && !title.isBlank() ? title.trim() : originalName);
        document.setFilename(storedName);
        document.setOriginalFilename(originalName);
        document.setType(resolveType(extension));
        document.setCategory(blankToNull(category));
        document.setDescription(blankToNull(description));
        document.setSize(file.getSize());
        document.setUploadedBy(current);
        document.setEntreprise(entreprise);

        Document saved = documentRepository.save(document);
        return documentMapper.toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public DocumentDownload download(Long id) {
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document introuvable"));
        assertCanAccess(document, resolveCurrentUser());
        Path filePath = storageRoot.resolve(document.getFilename()).normalize();
        try {
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists()) {
                throw new ResourceNotFoundException("Fichier introuvable");
            }
            String contentType = resolveContentType(filePath);
            return new DocumentDownload(resource, document.getOriginalFilename(), contentType);
        } catch (IOException ex) {
            throw new IllegalStateException("Impossible de lire le fichier", ex);
        }
    }

    @Override
    public void delete(Long id) {
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document introuvable"));
        Utilisateur current = resolveCurrentUser();
        if (!canDelete(document, current)) {
            throw new AccessDeniedException("Acces refuse");
        }
        Path filePath = storageRoot.resolve(document.getFilename()).normalize();
        try {
            Files.deleteIfExists(filePath);
        } catch (IOException ex) {
            throw new IllegalStateException("Impossible de supprimer le fichier", ex);
        }
        documentRepository.delete(document);
    }

    private void assertCanAccess(Document document, Utilisateur current) {
        if (isAdmin(current)) {
            return;
        }
        if ((isSecretary(current) || isAccountant(current)) && document.getUploadedBy() != null
                && Objects.equals(document.getUploadedBy().getId(), current.getId())) {
            return;
        }
        throw new AccessDeniedException("Acces refuse");
    }

    private boolean canDelete(Document document, Utilisateur current) {
        if (isAdmin(current)) {
            return true;
        }
        return (isSecretary(current) || isAccountant(current))
                && document.getUploadedBy() != null
                && Objects.equals(document.getUploadedBy().getId(), current.getId());
    }

    private Utilisateur resolveCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new SecurityException("No authenticated user");
        }
        return utilisateurRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new SecurityException("Authenticated user not found"));
    }

    private Entreprise resolveCurrentEntreprise(Utilisateur utilisateur) {
        if (utilisateur.getEntreprise() == null) {
            return entrepriseRepository.findByManager_Id(utilisateur.getId())
                    .orElseThrow(() -> new SecurityException("Entreprise introuvable pour l'utilisateur"));
        }
        return entrepriseRepository.findById(utilisateur.getEntreprise().getId())
                .orElseThrow(() -> new SecurityException("Entreprise introuvable pour l'utilisateur"));
    }

    private boolean isAdmin(Utilisateur utilisateur) {
        return utilisateur.getRole() == TypeRole.ROLE_ADMIN;
    }

    private boolean isSecretary(Utilisateur utilisateur) {
        return utilisateur.getRole() == TypeRole.ROLE_SECRETAIRE;
    }

    private boolean isAccountant(Utilisateur utilisateur) {
        return utilisateur.getRole() == TypeRole.ROLE_COMPTABLE;
    }

    private String resolveType(String extension) {
        if (extension == null) return "AUTRE";
        if (Set.of("png", "jpg", "jpeg").contains(extension)) {
            return "IMAGE";
        }
        return extension.toUpperCase(Locale.ROOT);
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
            return "document";
        }
        return filename.replaceAll("[\\\\/\n\r\t]", "_").trim();
    }

    private String blankToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
