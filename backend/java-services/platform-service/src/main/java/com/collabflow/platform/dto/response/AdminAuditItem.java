package com.collabflow.platform.dto.response;

import java.time.Instant;
import java.util.UUID;

public record AdminAuditItem(
        UUID id,
        UUID actorUserId,
        String action,
        String targetType,
        UUID targetId,
        String metadata,
        String ipAddress,
        Instant createdAt
) {}
