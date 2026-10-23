package com.collabflow.platform.dto.request;

import jakarta.validation.constraints.Size;

public record FeatureFlagRequest(
        boolean enabled,
        @Size(max = 500) String description
) {}
