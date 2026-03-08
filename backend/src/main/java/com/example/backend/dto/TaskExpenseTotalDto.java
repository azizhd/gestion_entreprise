package com.example.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskExpenseTotalDto {
    private Long taskId;
    private String taskDescription;
    private Double approvedTotal;
    private Double pendingTotal;
    private Double declinedTotal;
}
