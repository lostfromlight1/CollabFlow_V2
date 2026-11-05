package com.collabflow.platform.event;

import java.util.UUID;

public record ProjectCreatedPayload(
        UUID projectId,
        UUID workspaceId,
        String name,
        UUID ownerId,
        UUID organizationId,
        UUID actorId
) {}
