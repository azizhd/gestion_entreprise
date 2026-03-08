package com.example.backend.dto;

import com.example.backend.entitie.enumuration.RendezVousStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RendezVousStatusUpdateRequest {
    @NotNull
    private RendezVousStatus status;

    @Size(max = 500)
    private String note;
}
