package com.example.backend.dto;

import com.example.backend.entitie.enumuration.ModePaiement;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaiementDto {
    private Long id;
    @NotNull
    private LocalDate date;
    @NotNull
    @Positive
    private Double montant;
    @NotNull
    private ModePaiement modePaiement;
    private Long factureId;
    private Long fournisseurId;
    private Long entrepriseId;
}
