package com.collabflow.task.dto.request;

import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;

public record UpdateTaskRequest(
        @Size(max = 255) String name,
        @Size(max = 10000) String description,
        String priority,
        Instant startDate,
        Instant dueDate,
        BigDecimal estimatedHours,
        BigDecimal actualHours,
        Integer storyPoints
) {}
