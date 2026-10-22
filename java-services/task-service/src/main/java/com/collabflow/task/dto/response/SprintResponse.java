package com.collabflow.task.dto.response;

import com.collabflow.task.model.Sprint;
import java.time.Instant;
import java.util.UUID;

public record SprintResponse(
        UUID id,
        String name,
        String description,
        UUID projectId,
        UUID workspaceId,
        UUID organizationId,
        int sprintNumber,
        Instant startDate,
        Instant endDate,
        String status,
        String goal,
        Integer capacity,
        Integer velocity,
        int backlogOrder,
        Instant createdAt,
        Instant updatedAt
) {
    public static SprintResponse from(Sprint s) {
        return new SprintResponse(
                s.getId(),
                s.getName(),
                s.getDescription(),
                s.getProjectId(),
                s.getWorkspaceId(),
                s.getOrganizationId(),
                s.getSprintNumber(),
                s.getStartDate(),
                s.getEndDate(),
                s.getStatus(),
                s.getGoal(),
                s.getCapacity(),
                s.getVelocity(),
                s.getBacklogOrder(),
                s.getCreatedAt(),
                s.getUpdatedAt()
        );
    }
}
