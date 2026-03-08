package com.example.backend.service;

import com.example.backend.audit.ActionType;
import com.example.backend.audit.AuditAction;
import com.example.backend.entitie.Invitation;

public interface InvitationService {
    @AuditAction(action = "INVITATION_CREATE", entityType = "Invitation", type = ActionType.CREATE)
    Invitation createInvitation(Long inviterId, String invitedEmail);

    @AuditAction(action = "INVITATION_ACCEPT", entityType = "Invitation", type = ActionType.UPDATE)
    void acceptInvitation(String token, String password, String nom, String prenom);
}
