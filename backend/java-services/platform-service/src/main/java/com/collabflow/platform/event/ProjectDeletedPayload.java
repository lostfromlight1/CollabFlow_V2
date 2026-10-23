package com.collabflow.platform.event;

import java.util.UUID;

public record ProjectDeletedPayload(UUID projectId, UUID workspaceId) {}
