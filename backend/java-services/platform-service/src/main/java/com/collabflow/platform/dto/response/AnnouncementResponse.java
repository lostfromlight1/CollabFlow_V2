package com.collabflow.platform.dto.response;

import java.time.Instant;
import java.util.UUID;

public record AnnouncementResponse(
        UUID id,
        String title,
        String body,
        boolean active,
        Instant startsAt,
        Instant endsAt,
        UUID createdBy,
        Instant createdAt
) {}
