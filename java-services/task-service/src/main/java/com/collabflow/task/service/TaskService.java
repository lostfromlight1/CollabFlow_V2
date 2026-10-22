package com.collabflow.task.service;

import com.collabflow.common.api.PageResponse;
import com.collabflow.task.dto.request.AssignTaskRequest;
import com.collabflow.task.dto.request.ChangeTaskStatusRequest;
import com.collabflow.task.dto.request.CreateSubtaskRequest;
import com.collabflow.task.dto.request.CreateTaskRequest;
import com.collabflow.task.dto.request.UpdateTaskRequest;
import com.collabflow.task.dto.response.TaskResponse;
import com.collabflow.task.dto.response.TaskStatsResponse;
import java.util.List;
import java.util.UUID;

public interface TaskService {

    TaskResponse create(UUID actorId, CreateTaskRequest request);

    PageResponse<TaskResponse> listByProject(
            UUID projectId,
            UUID userId,
            UUID statusId,
            UUID assigneeId,
            String priority,
            UUID sprintId,
            int page,
            int size
    );

    TaskResponse get(UUID taskId, UUID userId);

    TaskResponse update(UUID taskId, UUID userId, UpdateTaskRequest request);

    void delete(UUID taskId, UUID userId);

    TaskResponse changeStatus(UUID taskId, UUID userId, ChangeTaskStatusRequest request);

    TaskResponse assign(UUID taskId, UUID userId, AssignTaskRequest request);

    TaskStatsResponse statsByProject(UUID projectId, UUID userId);

    List<TaskResponse> listSubtasks(UUID parentTaskId, UUID userId);

    TaskResponse createSubtask(UUID parentTaskId, UUID actorId, CreateSubtaskRequest request);
}
