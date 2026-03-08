package com.example.backend.mapper;

import com.example.backend.dto.DepenseResponse;
import com.example.backend.entitie.Depense;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface DepenseMapper {

    @Mapping(target = "status", source = "statusDepense")
    @Mapping(target = "taskId", source = "tache.id")
    @Mapping(target = "createdById", source = "createdBy.id")
    @Mapping(target = "createdByName", expression = "java(depense.getCreatedBy() != null ? depense.getCreatedBy().getNom() + ' ' + depense.getCreatedBy().getPrenom() : null)")
    @Mapping(target = "approvedById", source = "approvedBy.id")
    @Mapping(target = "approvedByName", expression = "java(depense.getApprovedBy() != null ? depense.getApprovedBy().getNom() + ' ' + depense.getApprovedBy().getPrenom() : null)")
    DepenseResponse toResponse(Depense depense);
}
