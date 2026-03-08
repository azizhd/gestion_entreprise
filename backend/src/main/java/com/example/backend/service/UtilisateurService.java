package com.example.backend.service;

import com.example.backend.audit.ActionType;
import com.example.backend.audit.AuditAction;
import com.example.backend.dto.UtilisateurDTO;
import com.example.backend.entitie.enumuration.TypeRole;
import java.util.List;

public interface UtilisateurService {
    @AuditAction(action = "USER_ASSIGN_ROLE", type = ActionType.UPDATE, entityType = "Utilisateur")
    UtilisateurDTO assignRole(Long userId, TypeRole newRole);
    List<UtilisateurDTO> getAllUsers();
    UtilisateurDTO getUserById(Long id);
    @AuditAction(action = "USER_UPDATE", type = ActionType.UPDATE, entityType = "Utilisateur")
    UtilisateurDTO updateUser(Long id, UtilisateurDTO utilisateurDTO);
    @AuditAction(action = "USER_DEACTIVATE", type = ActionType.UPDATE, entityType = "Utilisateur")
    void deactivateUser(Long id);
    @AuditAction(action = "USER_REACTIVATE", type = ActionType.UPDATE, entityType = "Utilisateur")
    void reactivateUser(Long id);
    @AuditAction(action = "USER_CREATE", type = ActionType.CREATE, entityType = "Utilisateur")
    UtilisateurDTO createUser(UtilisateurDTO utilisateurDTO, String password);
    @AuditAction(action = "USER_DELETE", type = ActionType.DELETE, entityType = "Utilisateur")
    void deleteUser(Long id);
}
