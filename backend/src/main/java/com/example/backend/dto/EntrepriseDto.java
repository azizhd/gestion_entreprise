package com.example.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EntrepriseDto {
    private int id;
    private String nom;
    private String logo;
    private String email;
    private String telephone;
    private String location;
    private Long managerId;
    private Long abonnementId;
}
