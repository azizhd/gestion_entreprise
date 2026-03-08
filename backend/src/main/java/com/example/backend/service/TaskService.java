package com.example.backend.service;

import com.example.backend.audit.ActionType;
import com.example.backend.audit.AuditAction;
import com.example.backend.dto.TaskProgressUpdateRequest;
import com.example.backend.dto.TaskRequest;
import com.example.backend.dto.TaskResponse;
import com.example.backend.dto.TaskStatusUpdateRequest;
import org.springframework.data.domain.Page;

public interface TaskService {

    @AuditAction(action = "TASK_CREATE", entityType = "Tache", type = ActionType.CREATE)
    TaskResponse create(TaskRequest request);

    @AuditAction(action = "TASK_UPDATE", entityType = "Tache", type = ActionType.UPDATE)
    TaskResponse update(Long id, TaskRequest request);

    @AuditAction(action = "TASK_DELETE", entityType = "Tache", type = ActionType.DELETE)
    void delete(Long id);

    TaskResponse getById(Long id);

    Page<TaskResponse> listForAdmin(int page, int size);

    Page<TaskResponse> listForCurrentUser(int page, int size);

    @AuditAction(action = "TASK_STATUS", entityType = "Tache", type = ActionType.UPDATE)
    TaskResponse updateStatus(Long id, TaskStatusUpdateRequest request);

    @AuditAction(action = "TASK_PROGRESS", entityType = "Tache", type = ActionType.UPDATE)
    TaskResponse updateProgress(Long id, TaskProgressUpdateRequest request);
}
