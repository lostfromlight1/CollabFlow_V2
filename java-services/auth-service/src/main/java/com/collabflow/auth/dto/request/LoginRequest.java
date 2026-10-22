package com.collabflow.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

@Schema(description = "Login with email + password; optional organizationId when user has multiple orgs")
public record LoginRequest(
        @NotBlank @Email
        @Schema(example = "kaung@example.com")
        String email,

        @NotBlank
        @Schema(example = "password123")
        String password,

        @Schema(description = "Optional org to enter when the user belongs to multiple")
        UUID organizationId
) {}
