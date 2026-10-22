package com.collabflow.auth.dto.request;

import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @Size(min = 1, max = 255) String name,
        @Size(max = 500) String avatarUrl
) {}
