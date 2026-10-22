package com.collabflow.platform.dto.request;

import jakarta.validation.constraints.Size;

public record UpdateOrganizationRequest(
        @Size(max = 255) String name,
        @Size(max = 2000) String description,
        @Size(max = 500) String logoUrl
) {}
