package com.example.backend.controller;

import com.example.backend.dto.TaskProgressUpdateRequest;
import com.example.backend.dto.TaskRequest;
import com.example.backend.dto.TaskResponse;
import com.example.backend.dto.TaskStatusUpdateRequest;
import com.example.backend.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','SECRETAIRE','COMPTABLE')")
    public ResponseEntity<Page<TaskResponse>> list(@RequestParam(defaultValue = "0") int page,
                                                   @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(taskService.listForAdmin(page, size));
    }

    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE','SECRETAIRE','COMPTABLE')")
    public ResponseEntity<Page<TaskResponse>> listMine(@RequestParam(defaultValue = "0") int page,
                                                       @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(taskService.listForCurrentUser(page, size));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE','SECRETAIRE','COMPTABLE')")
    public ResponseEntity<TaskResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(taskService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','SECRETAIRE')")
    public ResponseEntity<TaskResponse> create(@Valid @RequestBody TaskRequest request) {
        return ResponseEntity.ok(taskService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SECRETAIRE')")
    public ResponseEntity<TaskResponse> update(@PathVariable Long id, @Valid @RequestBody TaskRequest request) {
        return ResponseEntity.ok(taskService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        taskService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN','SECRETAIRE')")
    public ResponseEntity<TaskResponse> updateStatus(@PathVariable Long id, @Valid @RequestBody TaskStatusUpdateRequest request) {
        return ResponseEntity.ok(taskService.updateStatus(id, request));
    }

    @PatchMapping("/{id}/progress")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE','SECRETAIRE','COMPTABLE')")
    public ResponseEntity<TaskResponse> updateProgress(@PathVariable Long id, @Valid @RequestBody TaskProgressUpdateRequest request) {
        return ResponseEntity.ok(taskService.updateProgress(id, request));
    }
}
