package com.collabflow.platform.dto.request;

import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

public record UpdateProjectRequest(
        @Size(max = 255) String name,
        @Size(max = 2000) String description,
        @Size(max = 500) String imageUrl,
        String projectType,
        String status,
        UUID leadId,
        Instant startDate,
        Instant dueDate
) {}
