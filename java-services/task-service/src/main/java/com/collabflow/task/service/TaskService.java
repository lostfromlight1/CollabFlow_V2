package com.collabflow.task.service;

import com.collabflow.task.dto.request.AssignTaskRequest;
import com.collabflow.task.dto.request.ChangeTaskStatusRequest;
import com.collabflow.task.dto.request.CreateTaskRequest;
import com.collabflow.task.dto.request.UpdateTaskRequest;
import com.collabflow.task.dto.response.TaskResponse;
import java.util.List;
import java.util.UUID;

public interface TaskService {

    TaskResponse create(UUID actorId, CreateTaskRequest request);

    List<TaskResponse> listByProject(UUID projectId, UUID userId);

    TaskResponse get(UUID taskId, UUID userId);

    TaskResponse update(UUID taskId, UUID userId, UpdateTaskRequest request);

    void delete(UUID taskId, UUID userId);

    TaskResponse changeStatus(UUID taskId, UUID userId, ChangeTaskStatusRequest request);

    TaskResponse assign(UUID taskId, UUID userId, AssignTaskRequest request);
}
