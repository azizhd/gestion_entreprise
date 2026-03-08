package com.example.backend.mapper;

import com.example.backend.dto.DevisDto;
import com.example.backend.dto.LigneDevisDto;
import com.example.backend.dto.DevisCreateUpdateRequest;
import com.example.backend.entitie.Devis;
import com.example.backend.entitie.LigneDevis;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface DevisMapper {

    @Mapping(source = "client.id", target = "clientId")
    @Mapping(source = "lignesdevis", target = "lignes")
    @Mapping(source = "facture.id", target = "factureId")
    @Mapping(source = "numeroDevis", target = "numeroDevis")
    @Mapping(source = "totalHt", target = "totalHt")
    @Mapping(source = "totalTva", target = "totalTva")
    @Mapping(source = "totalTtc", target = "totalTtc")
    @Mapping(source = "tvaRate", target = "tvaRate")
    @Mapping(source = "montant", target = "montant")
    @Mapping(source = "statut", target = "statut")
    @Mapping(source = "transformeEnFacture", target = "transformeEnFacture")
    DevisDto toDto(Devis devis);

    @Mapping(source = "clientId", target = "client.id")
    @Mapping(source = "lignes", target = "lignesdevis")
    @Mapping(source = "factureId", target = "facture.id")
    @Mapping(source = "numeroDevis", target = "numeroDevis")
    @Mapping(source = "totalHt", target = "totalHt")
    @Mapping(source = "totalTva", target = "totalTva")
    @Mapping(source = "totalTtc", target = "totalTtc")
    @Mapping(source = "tvaRate", target = "tvaRate")
    @Mapping(source = "montant", target = "montant")
    @Mapping(source = "statut", target = "statut")
    @Mapping(source = "transformeEnFacture", target = "transformeEnFacture")
    Devis toEntity(DevisDto dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "numeroDevis", ignore = true)
    @Mapping(target = "totalHt", ignore = true)
    @Mapping(target = "totalTva", ignore = true)
    @Mapping(target = "totalTtc", ignore = true)
    @Mapping(target = "montant", ignore = true)
    @Mapping(target = "transformeEnFacture", ignore = true)
    @Mapping(target = "statut", ignore = true)
    @Mapping(target = "reference", ignore = true)
    @Mapping(target = "facture", ignore = true)
    @Mapping(target = "lignesdevis", ignore = true)
    @Mapping(target = "client", ignore = true)
    Devis toEntity(DevisCreateUpdateRequest request);

    void updateEntity(DevisCreateUpdateRequest request, @MappingTarget Devis devis);

    List<LigneDevisDto> toLigneDtoList(List<LigneDevis> lignes);
    List<LigneDevis> toLigneEntityList(List<LigneDevisDto> dtos);
}

