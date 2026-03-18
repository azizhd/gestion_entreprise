package com.example.backend.dto;

import java.time.LocalDateTime;

public record DocumentDto(
        Long id,
        String title,
        String filename,
        String originalFilename,
        String type,
        String category,
        String description,
        Long size,
        LocalDateTime uploadDate,
        Long uploadedById,
        String uploadedByName
) {}
