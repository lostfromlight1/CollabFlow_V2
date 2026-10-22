package com.collabflow.platform.event;

import java.util.UUID;

public record MemberRemovedPayload(
        UUID organizationId,
        UUID userId,
        UUID membershipId
) {}
