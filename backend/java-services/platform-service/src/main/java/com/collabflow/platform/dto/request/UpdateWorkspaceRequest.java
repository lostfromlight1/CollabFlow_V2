package com.collabflow.platform.dto.request;

import jakarta.validation.constraints.Size;

public record UpdateWorkspaceRequest(
        @Size(max = 255) String name,
        @Size(max = 2000) String description,
        @Size(max = 500) String imageUrl,
        @Size(max = 50) String timeZone,
        @Size(max = 20) String dateFormat
) {}
