package com.example.backend.mapper;

import com.example.backend.dto.ClientDto;
import com.example.backend.entitie.Client;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ClientMapper {

    ClientDto toDTO(Client client);

    Client toEntity(ClientDto dto);
}
