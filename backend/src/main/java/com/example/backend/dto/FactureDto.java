package com.example.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.example.backend.entitie.enumuration.StatutFacture;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FactureDto {
    private Long id;
    private String numeroFacture;
    private String reference;
    private LocalDateTime date;
    private LocalDate dueDate;
    private StatutFacture statututFacture;
    private Double totalHt;
    private Double totalTva;
    private Double totalTtc;
    private Double tvaRate;
    private Double montant;
    private Boolean payee;
    private Long devisId;
    private Long clientId;
    private String clientName;
    private List<Long> paiementIds;
}
