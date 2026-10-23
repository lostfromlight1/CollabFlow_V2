package com.collabflow.auth.dto.response;

import java.util.List;
import java.util.UUID;

public record AuthResponse(
        UUID userId,
        String email,
        String name,
        String accessToken,
        String refreshToken,
        String tokenType,
        UUID selectedOrganizationId,
        List<OrganizationSummary> organizations,
        boolean requiresOrganizationSelection
) {
    public static AuthResponse of(
            UUID userId,
            String email,
            String name,
            String accessToken,
            String refreshToken,
            UUID selectedOrganizationId,
            List<OrganizationSummary> organizations,
            boolean requiresOrganizationSelection
    ) {
        return new AuthResponse(
                userId,
                email,
                name,
                accessToken,
                refreshToken,
                "Bearer",
                selectedOrganizationId,
                organizations,
                requiresOrganizationSelection
        );
    }
}
