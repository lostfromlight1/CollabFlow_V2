package com.collabflow.auth.dto.response;

import java.util.UUID;

public record AuthResponse(
        UUID userId,
        String email,
        String name,
        String accessToken,
        String refreshToken,
        String tokenType
) {
    public static AuthResponse of(
            UUID userId,
            String email,
            String name,
            String accessToken,
            String refreshToken
    ) {
        return new AuthResponse(userId, email, name, accessToken, refreshToken, "Bearer");
    }
}
