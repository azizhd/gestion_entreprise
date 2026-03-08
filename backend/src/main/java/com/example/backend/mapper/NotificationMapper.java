package com.example.backend.mapper;

import com.example.backend.dto.NotificationDto;
import com.example.backend.entitie.Notification;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface NotificationMapper {

    @Mapping(source = "utilisateur.id", target = "utilisateurId")
    NotificationDto toDto(Notification notification);

    @Mapping(source = "utilisateurId", target = "utilisateur.id")
    Notification toEntity(NotificationDto dto);
}
