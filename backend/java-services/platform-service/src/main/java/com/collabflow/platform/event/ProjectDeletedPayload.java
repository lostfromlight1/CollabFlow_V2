package com.collabflow.platform.event;

import java.util.UUID;

/** Archive: the project row stays with status {@code ARCHIVED}. */
public record ProjectDeletedPayload(
        UUID projectId,
        UUID workspaceId,
        UUID organizationId,
        UUID actorId
) {}
