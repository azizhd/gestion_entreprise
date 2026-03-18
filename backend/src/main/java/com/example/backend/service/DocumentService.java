package com.example.backend.service;

import com.example.backend.dto.DocumentDownload;
import com.example.backend.dto.DocumentDto;
import java.time.LocalDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

public interface DocumentService {
    Page<DocumentDto> list(String type,
                           String category,
                           String search,
                           LocalDateTime fromDate,
                           LocalDateTime toDate,
                           Pageable pageable);

    DocumentDto upload(MultipartFile file,
                       String title,
                       String category,
                       String description);

    DocumentDownload download(Long id);

    void delete(Long id);
}
