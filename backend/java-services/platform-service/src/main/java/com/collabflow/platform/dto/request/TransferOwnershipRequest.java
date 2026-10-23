package com.collabflow.platform.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record TransferOwnershipRequest(
        @NotNull
        @Schema(description = "Existing org member who becomes the new Organization Owner")
        UUID newOwnerUserId
) {}
