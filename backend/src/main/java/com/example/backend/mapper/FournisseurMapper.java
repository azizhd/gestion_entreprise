package com.example.backend.mapper;

import com.example.backend.dto.FournisseurDto;
import com.example.backend.entitie.Fournisseur;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface FournisseurMapper {

    @Mapping(source = "entreprise.id", target = "entrepriseId")
    FournisseurDto toDTO(Fournisseur fournisseur);

    @Mapping(source = "entrepriseId", target = "entreprise.id")
    Fournisseur toEntity(FournisseurDto dto);
}
