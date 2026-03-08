package com.example.backend.mapper;

import com.example.backend.dto.EntrepriseDto;
import com.example.backend.entitie.Entreprise;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface EntrepriseMapper {

    @Mapping(source = "manager.id", target = "managerId")
    @Mapping(source = "abonnement.id", target = "abonnementId")
    EntrepriseDto toDTO(Entreprise entreprise);

    @Mapping(source = "managerId", target = "manager.id")
    @Mapping(source = "abonnementId", target = "abonnement.id")
    Entreprise toEntity(EntrepriseDto dto);
}
