package com.example.backend.mapper;

import com.example.backend.dto.TaskResponse;
import com.example.backend.dto.UtilisateurSummaryDto;
import com.example.backend.entitie.Tache;
import com.example.backend.entitie.Utilisateur;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TaskMapper {

    @Mapping(target = "startDate", source = "dateDebut")
    @Mapping(target = "endDate", source = "dateFin")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "assignees", source = "utilisateurs")
    @Mapping(target = "entrepriseId", source = "entreprise.id")
    TaskResponse toResponse(Tache entity);

    UtilisateurSummaryDto toUserSummary(Utilisateur utilisateur);
}
