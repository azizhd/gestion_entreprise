package com.example.backend.mapper;

import com.example.backend.dto.NewsLetterDto;
import com.example.backend.entitie.NewsLetter;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface NewsLetterMapper {
    NewsLetterDto toDto(NewsLetter entity);
    NewsLetter toEntity(NewsLetterDto dto);
}
