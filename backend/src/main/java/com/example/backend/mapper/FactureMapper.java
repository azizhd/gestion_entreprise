package com.example.backend.mapper;

import com.example.backend.dto.FactureDto;
import com.example.backend.entitie.Facture;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface FactureMapper {

    @Mapping(source = "devis.id", target = "devisId")
    @Mapping(source = "devis.client.id", target = "clientId")
    @Mapping(source = "devis.client.nom", target = "clientName")
    @Mapping(source = "paiements", target = "paiementIds", qualifiedByName = "mapPaiementsToIds")
    FactureDto toDTO(Facture facture);

    @Mapping(source = "devisId", target = "devis.id")
    Facture toEntity(FactureDto dto);

    @Named("mapPaiementsToIds")
    default List<Long> mapPaiementsToIds(List<com.example.backend.entitie.Paiement> paiements) {
        if (paiements == null) return null;
        return paiements.stream().map(com.example.backend.entitie.Paiement::getId).toList();
    }
}
