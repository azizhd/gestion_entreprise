package com.example.backend.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RendezVousRequest {
    @NotBlank
    private String titre;

    @NotNull
    @FutureOrPresent
    private LocalDateTime startDateTime;

    @NotNull
    @FutureOrPresent
    private LocalDateTime endDateTime;

    @Size(max = 255)
    private String location;

    @Size(max = 1000)
    private String notes;

    private Long clientId;

    @Size(max = 255)
    private String contactName;

    @Size(max = 255)
    private String contactEmail;

    @Size(max = 50)
    private String contactPhone;
}
