package com.example.backend.dto;

import com.example.backend.entitie.enumuration.FournisseurCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FournisseurSummaryDto {
    private Long id;
    private String nom;
    private String prenom;
    private Boolean actif;
    private FournisseurCategory category;
    private Double creditLimit;
    private Double totalDepenses;
    private Double totalPaid;
    private Double remainingBalance;
}
