package com.collabflow.task.event;

import java.util.UUID;

/**
 * Captured before the row is deleted. The consumer must not load {@code tasks}.
 */
public record TaskDeletedPayload(
        UUID taskId,
        UUID projectId,
        UUID organizationId,
        UUID workspaceId,
        String name,
        UUID actorId
) {}
