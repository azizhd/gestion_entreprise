package com.example.backend.dto;

import com.example.backend.entitie.enumuration.StatusTache;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskRequest {
    @NotBlank
    private String description;

    @NotNull
    @FutureOrPresent
    private LocalDate startDate;

    @NotNull
    private LocalDate endDate;

    private StatusTache status;

    private List<Long> assigneeIds;

    private Double budget;
}
