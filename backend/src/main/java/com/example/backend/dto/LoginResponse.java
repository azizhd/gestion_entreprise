package com.example.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponse {
    private String token;
    private String refreshToken;
    private Long userId;
    private String email;
    private String nom;
    private String prenom;
    private String role;
    private Integer entrepriseId;
    private String photo;
}
