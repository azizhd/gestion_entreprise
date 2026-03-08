package com.example.backend.mapper;

import com.example.backend.dto.UtilisateurDTO;
import com.example.backend.entitie.Utilisateur;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UtilisateurMapper {

    @Mapping(target = "entrepriseId", expression = "java(utilisateur.getEntreprise() != null ? Long.valueOf(utilisateur.getEntreprise().getId()) : null)")
    @Mapping(source = "taches", target = "tacheIds", qualifiedByName = "mapTachesToIds")
    UtilisateurDTO toDto(Utilisateur utilisateur);

    @Named("mapTachesToIds")
    default List<Long> mapTachesToIds(List<com.example.backend.entitie.Tache> taches) {
        if (taches == null) return null;
        return taches.stream().map(com.example.backend.entitie.Tache::getId).toList();
    }
}
