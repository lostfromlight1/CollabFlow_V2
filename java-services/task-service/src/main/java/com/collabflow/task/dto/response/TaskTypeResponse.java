package com.collabflow.task.dto.response;

import com.collabflow.task.model.TaskType;
import java.util.UUID;

public record TaskTypeResponse(
        UUID id,
        String name,
        String description,
        String icon,
        String color,
        boolean isSystem,
        UUID defaultStatusId
) {
    public static TaskTypeResponse from(TaskType t) {
        return new TaskTypeResponse(
                t.getId(),
                t.getName(),
                t.getDescription(),
                t.getIcon(),
                t.getColor(),
                t.isSystem(),
                t.getDefaultStatusId()
        );
    }
}
