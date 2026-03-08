package com.example.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClientDto {
    private Long id;
    private String adresse;
    private String telephone;
    private String email;
    private String nom;
    private Integer paymentTermsDays;
}
