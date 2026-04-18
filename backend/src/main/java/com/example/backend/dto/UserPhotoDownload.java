package com.example.backend.dto;

import org.springframework.core.io.Resource;

public record UserPhotoDownload(Resource resource, String filename, String contentType) {}
