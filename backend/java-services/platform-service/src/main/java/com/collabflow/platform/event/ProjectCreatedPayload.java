package com.collabflow.platform.event;

import java.util.UUID;

public record ProjectCreatedPayload(
        UUID projectId,
        UUID workspaceId,
        String name,
        UUID ownerId
) {}
