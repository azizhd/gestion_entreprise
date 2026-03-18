package com.example.backend.dto;

import org.springframework.core.io.Resource;

public record DocumentDownload(Resource resource, String filename, String contentType) {}
