package com.collabflow.task.dto.request;

import jakarta.validation.constraints.Size;
import java.time.Instant;

public record UpdateSprintRequest(
        @Size(max = 255) String name,
        @Size(max = 10000) String description,
        Instant startDate,
        Instant endDate,
        @Size(max = 10000) String goal,
        Integer capacity,
        Integer backlogOrder
) {}
