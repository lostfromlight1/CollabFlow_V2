package com.collabflow.task.dto.response;

import com.collabflow.task.model.WorkflowStatus;
import java.util.UUID;

public record WorkflowStatusResponse(
        UUID id,
        UUID workflowId,
        String name,
        int displayOrder,
        String color,
        String category
) {
    public static WorkflowStatusResponse from(WorkflowStatus s) {
        return new WorkflowStatusResponse(
                s.getId(),
                s.getWorkflowId(),
                s.getName(),
                s.getDisplayOrder(),
                s.getColor(),
                s.getCategory()
        );
    }
}
