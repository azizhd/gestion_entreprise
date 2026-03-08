package com.example.backend.service;

import com.example.backend.dto.RendezVousDto;
import com.example.backend.dto.RendezVousRequest;
import com.example.backend.dto.RendezVousStatusUpdateRequest;
import org.springframework.data.domain.Page;

public interface RendezVousService {
    Page<RendezVousDto> list(int page, int size);
    RendezVousDto create(RendezVousRequest request);
    RendezVousDto update(Long id, RendezVousRequest request);
    void delete(Long id);
    RendezVousDto updateStatus(Long id, RendezVousStatusUpdateRequest request);
}
