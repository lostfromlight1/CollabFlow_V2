package com.collabflow.task.event;

import java.util.UUID;

public record TaskCreatedPayload(
        UUID taskId,
        UUID projectId,
        UUID workspaceId,
        String name,
        UUID reporterId,
        UUID statusId
) {}
