package com.collabflow.platform.event;

import java.util.UUID;

public record ProjectMemberRemovedPayload(
        UUID projectId,
        UUID workspaceId,
        UUID userId,
        UUID membershipId
) {}
