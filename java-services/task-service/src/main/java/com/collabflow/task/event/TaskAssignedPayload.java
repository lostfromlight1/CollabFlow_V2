package com.collabflow.task.event;

import java.util.UUID;

public record TaskAssignedPayload(
        UUID taskId,
        UUID projectId,
        UUID assigneeId,
        UUID previousAssigneeId
) {}
