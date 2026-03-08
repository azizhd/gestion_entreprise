package com.example.backend.dto;

import com.example.backend.entitie.enumuration.FournisseurCategory;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FournisseurDto {
    private Long id;
    @NotBlank
    private String nom;
    @NotBlank
    private String prenom;
    private String telephone;
    @Email
    @NotBlank
    private String email;
    private String description;
    private Long entrepriseId;
    private Boolean actif;
    @PositiveOrZero
    private Double creditLimit;
    private FournisseurCategory category;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
