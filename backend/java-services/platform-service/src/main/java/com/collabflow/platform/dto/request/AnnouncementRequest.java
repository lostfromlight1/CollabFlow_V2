package com.collabflow.platform.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record AnnouncementRequest(
        @NotBlank @Size(max = 200) String title,
        @NotBlank String body,
        boolean active,
        Instant startsAt,
        Instant endsAt
) {}
