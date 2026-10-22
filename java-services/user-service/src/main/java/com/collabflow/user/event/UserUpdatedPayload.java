package com.collabflow.user.event;

import java.util.UUID;

/** Kafka {@code user.updated} data payload. */
public record UserUpdatedPayload(
        UUID userId,
        String email,
        String name
) {}
