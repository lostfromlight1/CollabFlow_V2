package com.collabflow.platform.event;

import java.util.UUID;

public record ProjectMemberAddedPayload(
        UUID projectId,
        UUID workspaceId,
        UUID userId,
        UUID roleId,
        UUID membershipId
) {}
