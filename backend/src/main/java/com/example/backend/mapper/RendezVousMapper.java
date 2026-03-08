package com.example.backend.mapper;

import com.example.backend.dto.RendezVousDto;
import com.example.backend.dto.RendezVousRequest;
import com.example.backend.entitie.Client;
import com.example.backend.entitie.RendezVous;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface RendezVousMapper {

    @Mapping(source = "client.id", target = "clientId")
    @Mapping(source = "client.nom", target = "clientName")
    RendezVousDto toDto(RendezVous entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "statusNote", ignore = true)
    @Mapping(target = "entreprise", ignore = true)
    @Mapping(target = "client", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    RendezVous toEntity(RendezVousRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "statusNote", ignore = true)
    @Mapping(target = "entreprise", ignore = true)
    @Mapping(target = "client", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    void updateEntity(RendezVousRequest request, @MappingTarget RendezVous entity);

    default void attachClient(RendezVous entity, Client client) {
        entity.setClient(client);
        if (client != null && entity.getContactName() == null) {
            entity.setContactName(client.getNom());
            entity.setContactEmail(client.getEmail());
            entity.setContactPhone(client.getTelephone());
        }
    }
}
