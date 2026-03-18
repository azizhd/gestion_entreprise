package com.example.backend.dto;

import jakarta.validation.constraints.Size;

public record DocumentUploadRequest(
        @Size(max = 150) String title,
        @Size(max = 50) String category,
        @Size(max = 2000) String description
) {}
