package com.collabflow.platform.dto.response;

import java.time.Instant;
import java.util.UUID;

public record AdminOrganizationSummary(
        UUID id,
        String name,
        String description,
        String slug,
        String plan,
        boolean active,
        Instant createdAt,
        UUID ownerId,
        String ownerName,
        String ownerEmail
) {}
