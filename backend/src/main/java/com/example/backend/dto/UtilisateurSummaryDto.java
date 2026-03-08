package com.example.backend.dto;

import com.example.backend.entitie.enumuration.TypeRole;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UtilisateurSummaryDto {
    private Long id;
    private String nom;
    private String prenom;
    private String email;
    private TypeRole role;
}
