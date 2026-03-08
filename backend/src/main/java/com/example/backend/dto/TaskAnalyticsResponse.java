package com.example.backend.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskAnalyticsResponse {
    private long overdueTasks;
    private Double averageCompletionDays;
    private List<EmployeeTaskCountDto> tasksPerEmployee;
    private List<TaskExpenseTotalDto> expensesPerTask;
}
