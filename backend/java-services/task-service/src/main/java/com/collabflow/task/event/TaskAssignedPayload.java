package com.collabflow.task.event;

import java.util.UUID;

/** Kafka {@code task.assigned} data — matches docs/microservice.md schema. */
public record TaskAssignedPayload(
        UUID taskId,
        UUID projectId,
        UUID workspaceId,
        UUID organizationId,
        UUID assigneeId,
        UUID previousAssigneeId,
        UUID assignerId
) {}
