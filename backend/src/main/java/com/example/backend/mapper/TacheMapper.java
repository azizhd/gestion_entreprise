package com.example.backend.mapper;

import com.example.backend.dto.TacheDto;
import com.example.backend.entitie.Tache;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TacheMapper {

    @Mapping(source = "utilisateurs", target = "utilisateurIds", qualifiedByName = "mapUtilisateursToIds")
    @Mapping(source = "depenses", target = "depenseIds", qualifiedByName = "mapDepensesToIds")
    TacheDto toDTO(Tache tache);

    Tache toEntity(TacheDto dto);

    @Named("mapUtilisateursToIds")
    default List<Long> mapUtilisateursToIds(List<com.example.backend.entitie.Utilisateur> utilisateurs) {
        if (utilisateurs == null) return null;
        return utilisateurs.stream().map(com.example.backend.entitie.Utilisateur::getId).toList();
    }

    @Named("mapDepensesToIds")
    default List<Long> mapDepensesToIds(List<com.example.backend.entitie.Depense> depenses) {
        if (depenses == null) return null;
        return depenses.stream().map(com.example.backend.entitie.Depense::getId).toList();
    }
}
