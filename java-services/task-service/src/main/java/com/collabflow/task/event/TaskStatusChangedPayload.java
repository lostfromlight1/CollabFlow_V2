package com.collabflow.task.event;

import java.util.UUID;

public record TaskStatusChangedPayload(
        UUID taskId,
        UUID projectId,
        UUID fromStatusId,
        UUID toStatusId
) {}
