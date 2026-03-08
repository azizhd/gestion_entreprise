package com.example.backend.dto;

import com.example.backend.entitie.enumuration.AbonnementType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AbonnementDto {
    private Long id;
    private AbonnementType abonnementType;
    private LocalDateTime dateDebut;
    private LocalDateTime dateFin;
    private Boolean status;
}
