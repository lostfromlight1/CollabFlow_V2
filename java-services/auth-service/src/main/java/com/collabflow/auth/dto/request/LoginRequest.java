package com.collabflow.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Login credentials.
 * <p>
 * TODO (later): add organization (id or slug). Login will be org-scoped —
 * email + password alone is not enough once multi-tenant org selection ships.
 * Leave as email + password for MVP Week 3.
 */
@Schema(description = "Login with email + password. Organization scope comes later.")
public record LoginRequest(
        @NotBlank @Email
        @Schema(example = "kaung@example.com")
        String email,

        @NotBlank
        @Schema(example = "password123")
        String password
) {}
