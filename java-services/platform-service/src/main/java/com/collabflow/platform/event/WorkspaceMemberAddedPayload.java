package com.collabflow.platform.event;

import java.util.UUID;

public record WorkspaceMemberAddedPayload(
        UUID workspaceId,
        UUID organizationId,
        UUID userId,
        UUID roleId,
        UUID membershipId
) {}
