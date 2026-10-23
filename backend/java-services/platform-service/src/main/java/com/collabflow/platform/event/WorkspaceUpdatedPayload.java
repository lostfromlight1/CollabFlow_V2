package com.collabflow.platform.event;

import java.util.UUID;

public record WorkspaceUpdatedPayload(UUID workspaceId, UUID organizationId, String name) {}
