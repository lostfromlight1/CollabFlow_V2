package com.collabflow.task.dto.response;

import com.collabflow.task.model.StatusWorkflow;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record WorkflowResponse(
        UUID id,
        String name,
        String description,
        UUID workspaceId,
        boolean isDefault,
        boolean isTemplate,
        Instant createdAt,
        List<WorkflowStatusResponse> statuses
) {
    public static WorkflowResponse from(StatusWorkflow w, List<WorkflowStatusResponse> statuses) {
        return new WorkflowResponse(
                w.getId(),
                w.getName(),
                w.getDescription(),
                w.getWorkspaceId(),
                w.isDefaultWorkflow(),
                w.isTemplate(),
                w.getCreatedAt(),
                statuses
        );
    }
}
