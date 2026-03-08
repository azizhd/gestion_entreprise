package com.example.backend.mapper;

import com.example.backend.dto.PaiementDto;
import com.example.backend.entitie.Paiement;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PaiementMapper {

    @Mapping(source = "facture.id", target = "factureId")
    @Mapping(source = "fournisseur.id", target = "fournisseurId")
    @Mapping(source = "entreprise.id", target = "entrepriseId")
    PaiementDto toDTO(Paiement paiement);

    @Mapping(source = "factureId", target = "facture.id")
    @Mapping(source = "fournisseurId", target = "fournisseur.id")
    @Mapping(source = "entrepriseId", target = "entreprise.id")
    Paiement toEntity(PaiementDto dto);
}
