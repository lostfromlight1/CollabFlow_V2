package com.collabflow.task.dto.response;

import com.collabflow.task.model.Task;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TaskResponse(
        UUID id,
        String name,
        String description,
        UUID taskTypeId,
        UUID projectId,
        UUID workspaceId,
        UUID organizationId,
        UUID parentTaskId,
        UUID statusId,
        UUID assigneeId,
        UUID reporterId,
        String priority,
        int position,
        Instant startDate,
        Instant dueDate,
        BigDecimal estimatedHours,
        BigDecimal actualHours,
        Integer storyPoints,
        UUID sprintId,
        Instant createdAt,
        Instant updatedAt,
        Instant closedAt
) {
    public static TaskResponse from(Task t) {
        return new TaskResponse(
                t.getId(),
                t.getName(),
                t.getDescription(),
                t.getTaskTypeId(),
                t.getProjectId(),
                t.getWorkspaceId(),
                t.getOrganizationId(),
                t.getParentTaskId(),
                t.getStatusId(),
                t.getAssigneeId(),
                t.getReporterId(),
                t.getPriority(),
                t.getPosition(),
                t.getStartDate(),
                t.getDueDate(),
                t.getEstimatedHours(),
                t.getActualHours(),
                t.getStoryPoints(),
                t.getSprintId(),
                t.getCreatedAt(),
                t.getUpdatedAt(),
                t.getClosedAt()
        );
    }
}
