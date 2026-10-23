package com.collabflow.task.service;

import com.collabflow.task.dto.request.AttachTaskToSprintRequest;
import com.collabflow.task.dto.request.CreateSprintRequest;
import com.collabflow.task.dto.request.UpdateSprintRequest;
import com.collabflow.task.dto.response.SprintResponse;
import com.collabflow.task.dto.response.TaskResponse;
import java.util.List;
import java.util.UUID;

public interface SprintService {

    SprintResponse create(UUID actorId, CreateSprintRequest request);

    List<SprintResponse> listByProject(UUID projectId, UUID userId);

    SprintResponse get(UUID sprintId, UUID userId);

    SprintResponse update(UUID sprintId, UUID userId, UpdateSprintRequest request);

    void delete(UUID sprintId, UUID userId);

    SprintResponse start(UUID sprintId, UUID userId);

    SprintResponse complete(UUID sprintId, UUID userId);

    TaskResponse attachTask(UUID sprintId, UUID userId, AttachTaskToSprintRequest request);

    TaskResponse detachTask(UUID sprintId, UUID taskId, UUID userId);
}
