package com.example.backend.mapper;

import com.example.backend.dto.DocumentDto;
import com.example.backend.entitie.Document;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface DocumentMapper {

    @Mapping(target = "uploadedById", source = "uploadedBy.id")
    @Mapping(target = "uploadedByName", expression = "java(document.getUploadedBy() != null ? document.getUploadedBy().getNom() + ' ' + document.getUploadedBy().getPrenom() : null)")
    DocumentDto toDto(Document document);
}
