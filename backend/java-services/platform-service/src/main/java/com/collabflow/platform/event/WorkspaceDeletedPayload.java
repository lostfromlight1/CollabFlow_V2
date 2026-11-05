package com.collabflow.platform.event;

import java.util.UUID;

/** Soft delete: the workspace row stays with {@code is_active = false}. */
public record WorkspaceDeletedPayload(UUID workspaceId, UUID organizationId, UUID actorId) {}
