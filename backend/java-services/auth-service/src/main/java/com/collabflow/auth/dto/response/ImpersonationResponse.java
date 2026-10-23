package com.collabflow.auth.dto.response;

import java.time.Instant;
import java.util.UUID;

public record ImpersonationResponse(
        String accessToken,
        Instant expiresAt,
        UUID targetUserId,
        String targetEmail,
        String targetName
) {}
