package com.collabflow.auth.dto.response;

import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        String name,
        boolean emailVerified,
        boolean active
) {}
