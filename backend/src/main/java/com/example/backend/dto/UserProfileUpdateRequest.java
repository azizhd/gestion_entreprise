package com.example.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileUpdateRequest {
    private String nom;
    private String prenom;

    @Email(message = "Email must be valid")
    private String email;

    private String currentPassword;

    @Size(min = 8, message = "Password must be at least 8 characters")
    private String newPassword;
}
