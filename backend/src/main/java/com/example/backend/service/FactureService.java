package com.example.backend.service;

import com.example.backend.audit.ActionType;
import com.example.backend.audit.AuditAction;
import com.example.backend.dto.FactureDto;
import org.springframework.data.domain.Page;

public interface FactureService {

    Page<FactureDto> listFactures(int page, int size, String statutFilter);

    FactureDto getFacture(Long id);

    @AuditAction(action = "FACTURE_SEND", entityType = "Facture", type = ActionType.UPDATE)
    void sendFactureEmail(Long id);

    @AuditAction(action = "FACTURE_MARK_PAID", entityType = "Facture", type = ActionType.UPDATE)
    FactureDto markAsPaid(Long id);
}
