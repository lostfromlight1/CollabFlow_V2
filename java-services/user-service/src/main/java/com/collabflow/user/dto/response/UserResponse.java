package com.collabflow.user.dto.response;

import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        String name,
        String avatarUrl,
        boolean emailVerified,
        boolean active,
        boolean masterAdmin
) {}
