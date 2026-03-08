package com.example.backend.service.impl;

import com.example.backend.audit.AuditAction;
import com.example.backend.audit.ActionType;
import com.example.backend.dto.UtilisateurDTO;
import com.example.backend.entitie.Entreprise;
import com.example.backend.entitie.Utilisateur;
import com.example.backend.entitie.enumuration.TypeRole;
import com.example.backend.mapper.UtilisateurMapper;
import com.example.backend.repository.EntrepriseRepository;
import com.example.backend.repository.UtilisateurRepository;
import com.example.backend.service.UtilisateurService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class UtilisateurServiceImpl implements UtilisateurService {

    private final UtilisateurRepository utilisateurRepository;
    private final UtilisateurMapper utilisateurMapper;
    private final PasswordEncoder passwordEncoder;
    private final EntrepriseRepository entrepriseRepository;

    public UtilisateurServiceImpl(UtilisateurRepository utilisateurRepository,
                                  UtilisateurMapper utilisateurMapper,
                                  PasswordEncoder passwordEncoder,
                                  EntrepriseRepository entrepriseRepository) {
        this.utilisateurRepository = utilisateurRepository;
        this.utilisateurMapper = utilisateurMapper;
        this.passwordEncoder = passwordEncoder;
        this.entrepriseRepository = entrepriseRepository;
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
        return entrepriseRepository.findByManager_Id(currentUser.getId())
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
}
