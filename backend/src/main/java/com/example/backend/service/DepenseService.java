package com.example.backend.service;

import com.example.backend.audit.ActionType;
import com.example.backend.audit.AuditAction;
import com.example.backend.dto.DepenseDecisionRequest;
import com.example.backend.dto.DepenseRequest;
import com.example.backend.dto.DepenseResponse;
import org.springframework.data.domain.Page;

public interface DepenseService {

    @AuditAction(action = "DEPENSE_SUBMIT_TASK", type = ActionType.CREATE, entityType = "Depense")
    DepenseResponse submitExpense(Long taskId, DepenseRequest request);

    @AuditAction(action = "DEPENSE_SUBMIT_FOURNISSEUR", type = ActionType.CREATE, entityType = "Depense")
    DepenseResponse submitExpenseForFournisseur(Long fournisseurId, Long entrepriseId, DepenseRequest request);

    @AuditAction(action = "DEPENSE_DECISION", type = ActionType.UPDATE, entityType = "Depense")
    DepenseResponse approveOrDecline(Long depenseId, DepenseDecisionRequest request);

    Page<DepenseResponse> listByTask(Long taskId, int page, int size);
}
