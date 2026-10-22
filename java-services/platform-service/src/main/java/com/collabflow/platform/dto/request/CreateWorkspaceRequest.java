package com.collabflow.platform.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CreateWorkspaceRequest(
        @NotNull
        @Schema(description = "Parent organization id")
        UUID organizationId,

        @NotBlank @Size(max = 255)
        @Schema(example = "Engineering")
        String name,

        @Size(max = 2000)
        String description,

        @Size(max = 50)
        String timeZone,

        @Size(max = 20)
        String dateFormat
) {}
