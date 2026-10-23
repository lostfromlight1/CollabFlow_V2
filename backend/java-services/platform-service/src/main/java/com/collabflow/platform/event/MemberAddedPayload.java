package com.collabflow.platform.event;

import java.util.UUID;

public record MemberAddedPayload(UUID organizationId, UUID userId, UUID roleId, UUID memberId) {}
