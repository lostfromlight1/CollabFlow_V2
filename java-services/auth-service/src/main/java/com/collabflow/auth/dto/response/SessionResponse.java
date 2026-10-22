package com.collabflow.auth.dto.response;

import java.time.Instant;
import java.util.UUID;

/** Active refresh-token session — never includes the raw token. */
public record SessionResponse(
        UUID id,
        Instant createdAt,
        Instant expiresAt
) {}
