package com.example.backend.dto;

import com.example.backend.entitie.enumuration.StatusDepense;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DepenseDto {
    private Long id;
    private String desciption;
    private Double montant;
    private LocalDate date;
    private StatusDepense statusDepense;
    private Long fournisseurId;
    private Long tacheId;
}
