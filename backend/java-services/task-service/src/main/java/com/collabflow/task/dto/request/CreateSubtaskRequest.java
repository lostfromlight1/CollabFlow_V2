package com.collabflow.task.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CreateSubtaskRequest(
        @NotBlank @Size(max = 255)
        @Schema(example = "Write unit tests")
        String name,

        @Size(max = 10000)
        String description,

        @Schema(description = "Defaults to system Task type")
        UUID taskTypeId,

        @Schema(allowableValues = {"LOW", "MEDIUM", "HIGH", "CRITICAL"})
        String priority,

        Instant startDate,

        Instant dueDate,

        BigDecimal estimatedHours,

        Integer storyPoints
) {}
