package com.example.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LigneDevisRequest {
    private Long id;

    @NotBlank
    private String description;

    @NotNull
    @Positive
    private Integer quantite;

    @NotNull
    @Positive
    private Double prixUnitaire;
}
