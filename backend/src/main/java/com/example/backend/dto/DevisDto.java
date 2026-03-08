package com.example.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.example.backend.entitie.enumuration.StatutDevis;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DevisDto {
    private Long id;
    private String reference;
    private String numeroDevis;
    private LocalDateTime devisDate;
    private Double totalHt;
    private Double totalTva;
    private Double totalTtc;
    private Double tvaRate;
    private Double montant;
    private StatutDevis statut;
    private Boolean transformeEnFacture;
    private Long clientId;
    private List<LigneDevisDto> lignes;
    private Long factureId;
}
