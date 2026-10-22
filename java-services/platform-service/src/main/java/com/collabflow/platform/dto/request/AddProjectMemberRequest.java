package com.collabflow.platform.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AddProjectMemberRequest(
        @NotNull
        @Schema(description = "User must already be a workspace member")
        UUID userId,

        @NotNull
        @Schema(example = "a0000000-0000-4000-8000-000000000008")
        UUID roleId
) {}
