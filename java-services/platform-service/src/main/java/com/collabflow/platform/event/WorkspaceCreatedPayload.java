package com.collabflow.platform.event;

import java.util.UUID;

public record WorkspaceCreatedPayload(
        UUID workspaceId,
        UUID organizationId,
        String name,
        UUID ownerId
) {}
