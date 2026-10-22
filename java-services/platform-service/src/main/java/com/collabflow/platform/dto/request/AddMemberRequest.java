package com.collabflow.platform.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AddMemberRequest(
        @NotNull
        @Schema(description = "Existing user id from auth-service")
        UUID userId,

        @NotNull
        @Schema(description = "Organization-scoped role id", example = "a0000000-0000-4000-8000-000000000003")
        UUID roleId
) {}
