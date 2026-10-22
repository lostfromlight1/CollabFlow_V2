package com.collabflow.platform.event;

import java.util.UUID;

public record ProjectUpdatedPayload(UUID projectId, UUID workspaceId, String name) {}
