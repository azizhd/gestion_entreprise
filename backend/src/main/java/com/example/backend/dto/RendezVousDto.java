package com.example.backend.dto;

import com.example.backend.entitie.enumuration.RendezVousStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RendezVousDto {
    private Long id;
    private String titre;
    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;
    private String location;
    private String notes;
    private RendezVousStatus status;
    private String statusNote;
    private Long clientId;
    private String clientName;
    private String contactName;
    private String contactEmail;
    private String contactPhone;
}
