package com.example.backend.mapper;

import com.example.backend.dto.AuditLogDto;
import com.example.backend.entitie.AuditLog;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AuditLogMapper {

    @Mapping(source = "utilisateur.id", target = "utilisateurId")
    @Mapping(source = "utilisateur.email", target = "utilisateurEmail")
    @Mapping(source = "actionType", target = "actionType")
    AuditLogDto toDTO(AuditLog auditLog);
}
