package com.collabflow.task.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CreateTaskRequest(
        @NotNull UUID projectId,

        @NotBlank @Size(max = 255)
        @Schema(example = "Implement login page")
        String name,

        @Size(max = 10000)
        String description,

        @Schema(description = "Defaults to system Task type")
        UUID taskTypeId,

        UUID parentTaskId,

        @Schema(allowableValues = {"LOW", "MEDIUM", "HIGH", "CRITICAL"})
        String priority,

        Instant startDate,

        Instant dueDate,

        BigDecimal estimatedHours,

        Integer storyPoints
) {}
