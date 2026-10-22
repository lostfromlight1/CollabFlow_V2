package com.collabflow.platform.dto.response;

import com.collabflow.platform.model.Workspace;
import java.time.Instant;
import java.util.UUID;

public record WorkspaceResponse(
        UUID id,
        String name,
        String description,
        String imageUrl,
        UUID organizationId,
        UUID ownerId,
        String inviteCode,
        UUID defaultWorkflowId,
        String timeZone,
        String dateFormat,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
    public static WorkspaceResponse from(Workspace w) {
        return new WorkspaceResponse(
                w.getId(),
                w.getName(),
                w.getDescription(),
                w.getImageUrl(),
                w.getOrganizationId(),
                w.getOwnerId(),
                w.getInviteCode(),
                w.getDefaultWorkflowId(),
                w.getTimeZone(),
                w.getDateFormat(),
                w.isActive(),
                w.getCreatedAt(),
                w.getUpdatedAt()
        );
    }
}
