package com.collabflow.task.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

public record CreateSprintRequest(
        @NotNull UUID projectId,

        @NotBlank @Size(max = 255)
        @Schema(example = "Sprint 1")
        String name,

        @Size(max = 10000)
        String description,

        @NotNull Instant startDate,

        @NotNull Instant endDate,

        @Size(max = 10000)
        String goal,

        Integer capacity,

        Integer backlogOrder
) {}
