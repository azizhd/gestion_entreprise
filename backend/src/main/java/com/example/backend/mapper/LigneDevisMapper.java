package com.example.backend.mapper;

import com.example.backend.dto.LigneDevisDto;
import com.example.backend.entitie.LigneDevis;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface LigneDevisMapper {

    @Mapping(source = "devis.id", target = "devisId")
    LigneDevisDto toDTO(LigneDevis ligneDevis);

    @Mapping(source = "devisId", target = "devis.id")
    LigneDevis toEntity(LigneDevisDto dto);
}
