package com.collabflow.platform.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AddWorkspaceMemberRequest(
        @NotNull
        @Schema(description = "Existing user id (must already be org member)")
        UUID userId,

        @NotNull
        @Schema(description = "WORKSPACE-scoped role id", example = "a0000000-0000-4000-8000-000000000005")
        UUID roleId
) {}
