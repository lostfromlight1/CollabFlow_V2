package com.collabflow.user.event;

import java.util.UUID;

/** Kafka {@code user.created} data payload. */
public record UserCreatedPayload(
        UUID userId,
        String email,
        String name
) {}
