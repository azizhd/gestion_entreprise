package com.example.backend.service;

import com.example.backend.audit.ActionType;
import com.example.backend.audit.AuditAction;

public interface NewsLetterService {
    /**
     * Send the newsletter identified by id to all associated clients (by email).
     */
    @AuditAction(action = "NEWSLETTER_SEND", entityType = "NewsLetter", type = ActionType.CREATE)
    void sendEmailToClients(Long newsLetterId);
}
