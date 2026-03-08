package com.example.backend.dto;

import com.example.backend.entitie.enumuration.StatusTache;
import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskResponse {
    private Long id;
    private String description;
    private LocalDate startDate;
    private LocalDate endDate;
    private StatusTache status;
    private Integer progress;
    private Double budget;
    private Double totalApprovedExpenses;
    private Double totalPendingExpenses;
    private Double totalDeclinedExpenses;
    private Long entrepriseId;
    private List<UtilisateurSummaryDto> assignees;
}
