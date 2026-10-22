package com.collabflow.platform.dto.response;

import com.collabflow.platform.model.Project;
import java.time.Instant;
import java.util.UUID;

public record ProjectResponse(
        UUID id,
        String name,
        String description,
        String imageUrl,
        UUID workspaceId,
        UUID parentProjectId,
        UUID ownerId,
        String status,
        String projectType,
        Instant startDate,
        Instant dueDate,
        UUID workflowId,
        UUID leadId,
        Instant createdAt,
        Instant updatedAt
) {
    public static ProjectResponse from(Project p) {
        return new ProjectResponse(
                p.getId(),
                p.getName(),
                p.getDescription(),
                p.getImageUrl(),
                p.getWorkspaceId(),
                p.getParentProjectId(),
                p.getOwnerId(),
                p.getStatus(),
                p.getProjectType(),
                p.getStartDate(),
                p.getDueDate(),
                p.getWorkflowId(),
                p.getLeadId(),
                p.getCreatedAt(),
                p.getUpdatedAt()
        );
    }
}
