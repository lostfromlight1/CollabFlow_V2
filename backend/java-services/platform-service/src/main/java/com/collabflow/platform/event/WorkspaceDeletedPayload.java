package com.collabflow.platform.event;

import java.util.UUID;

public record WorkspaceDeletedPayload(UUID workspaceId, UUID organizationId) {}
