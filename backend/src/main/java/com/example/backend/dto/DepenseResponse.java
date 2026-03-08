package com.example.backend.dto;

import com.example.backend.entitie.enumuration.StatusDepense;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DepenseResponse {
    private Long id;
    private String description;
    private Double montant;
    private LocalDate date;
    private StatusDepense status;
    private Long taskId;
    private Long createdById;
    private String createdByName;
    private Long approvedById;
    private String approvedByName;
    private LocalDateTime approvalDate;
    private String rejectionReason;
}
