package com.example.backend.controller;

import com.example.backend.dto.TaskAnalyticsResponse;
import com.example.backend.service.TaskAnalyticsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tasks/analytics")
@PreAuthorize("hasRole('ADMIN')")
public class TaskAnalyticsController {

    private final TaskAnalyticsService taskAnalyticsService;

    public TaskAnalyticsController(TaskAnalyticsService taskAnalyticsService) {
        this.taskAnalyticsService = taskAnalyticsService;
    }

    @GetMapping
    public ResponseEntity<TaskAnalyticsResponse> getAnalytics() {
        return ResponseEntity.ok(taskAnalyticsService.computeAnalytics());
    }
}
