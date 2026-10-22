package com.collabflow.platform.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record JoinWorkspaceRequest(
        @NotBlank
        @Size(min = 6, max = 20)
        @Schema(example = "a1b2c3d4")
        String inviteCode
) {}
