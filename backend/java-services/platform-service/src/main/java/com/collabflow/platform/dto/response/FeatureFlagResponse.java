package com.collabflow.platform.dto.response;

import java.time.Instant;

public record FeatureFlagResponse(
        String key,
        boolean enabled,
        String description,
        Instant updatedAt
) {}
