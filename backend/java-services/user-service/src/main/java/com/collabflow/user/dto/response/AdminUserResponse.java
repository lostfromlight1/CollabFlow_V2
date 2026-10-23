package com.collabflow.user.dto.response;

import java.time.Instant;
import java.util.UUID;

public record AdminUserResponse(
        UUID id,
        String email,
        String name,
        String avatarUrl,
        boolean emailVerified,
        boolean active,
        boolean masterAdmin,
        Instant createdAt,
        Instant lastLoginAt,
        Instant deletedAt
) {}
