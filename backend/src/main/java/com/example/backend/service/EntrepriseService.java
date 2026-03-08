package com.example.backend.service;

import com.example.backend.audit.ActionType;
import com.example.backend.audit.AuditAction;
import com.example.backend.dto.EntrepriseDto;
import java.util.List;

public interface EntrepriseService {
    List<EntrepriseDto> getAll();
    EntrepriseDto getById(Integer id);
    @AuditAction(action = "ENTREPRISE_CREATE", type = ActionType.CREATE, entityType = "Entreprise")
    EntrepriseDto create(EntrepriseDto dto);
    @AuditAction(action = "ENTREPRISE_UPDATE", type = ActionType.UPDATE, entityType = "Entreprise")
    EntrepriseDto update(Integer id, EntrepriseDto dto);
    @AuditAction(action = "ENTREPRISE_DELETE", type = ActionType.DELETE, entityType = "Entreprise")
    void delete(Integer id);
}
