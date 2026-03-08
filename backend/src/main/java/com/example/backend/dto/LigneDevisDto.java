package com.example.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LigneDevisDto {
    private Long id;
    private Long devisId;
    private String description;
    private Integer quantite;
    private Double prixUnitaire;
    private Double total;
}
