package com.example.backend.dto;

import com.example.backend.entitie.enumuration.StatusTache;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskStatusUpdateRequest {
    @NotNull
    private StatusTache status;
}
