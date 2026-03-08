package com.example.backend.dto;

import com.example.backend.entitie.enumuration.TypeRole;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UtilisateurDTO {
    private Long id;
    private String nom;
    private String prenom;
    private String email;
    private TypeRole role;
    private String photo;
    private String telephone;

    private Long entrepriseId; // only store entreprise ID
    private List<Long> tacheIds; // store task IDs assigned
}
