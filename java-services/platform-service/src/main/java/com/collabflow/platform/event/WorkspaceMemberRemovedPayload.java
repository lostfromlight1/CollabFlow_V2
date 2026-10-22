package com.collabflow.platform.event;

import java.util.UUID;

public record WorkspaceMemberRemovedPayload(
        UUID workspaceId,
        UUID organizationId,
        UUID userId,
        UUID membershipId
) {}
