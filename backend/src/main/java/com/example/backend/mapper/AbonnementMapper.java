package com.example.backend.mapper;

import com.example.backend.dto.AbonnementDto;
import com.example.backend.entitie.Abonnement;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AbonnementMapper {

    AbonnementDto toDTO(Abonnement abonnement);

    Abonnement toEntity(AbonnementDto dto);
}
