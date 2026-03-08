package com.example.backend.service;

import com.example.backend.audit.ActionType;
import com.example.backend.audit.AuditAction;
import com.example.backend.dto.DevisCreateUpdateRequest;
import com.example.backend.dto.DevisDto;
import com.example.backend.dto.FactureDto;
import org.springframework.data.domain.Page;

public interface DevisService {
    Page<DevisDto> getAllDevis(int page, int size);

    DevisDto getDevisById(Long id);

    @AuditAction(action = "DEVIS_CREATE", entityType = "Devis", type = ActionType.CREATE)
    DevisDto createDevis(DevisCreateUpdateRequest devisDto);

    @AuditAction(action = "DEVIS_UPDATE", entityType = "Devis", type = ActionType.UPDATE)
    DevisDto updateDevis(Long id, DevisCreateUpdateRequest devisDto);

    @AuditAction(action = "DEVIS_DELETE", entityType = "Devis", type = ActionType.DELETE)
    void deleteDevis(Long id);

    @AuditAction(action = "DEVIS_TRANSFORM", entityType = "Devis", type = ActionType.UPDATE)
    FactureDto transformerEnFacture(Long id);
}
