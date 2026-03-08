package com.example.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeTaskCountDto {
    private Long employeeId;
    private String employeeName;
    private long taskCount;
}
