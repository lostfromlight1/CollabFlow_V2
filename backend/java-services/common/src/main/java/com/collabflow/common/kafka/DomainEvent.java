package com.collabflow.common.kafka;

import java.time.Instant;
import java.util.UUID;

/**
 * Standard CollabFlow Kafka envelope (see docs/microservice.md).
 *
 * <p>Why a shared shape: producers (Java) and consumers (Go later) agree on
 * {@code eventId}, {@code eventType}, {@code timestamp}, {@code version}, {@code data}.
 */
public record DomainEvent<T>(
        String eventId,
        String eventType,
        Instant timestamp,
        String version,
        T data
) {
    public static final String VERSION = "1.0";

    public static <T> DomainEvent<T> of(String eventType, T data) {
        return new DomainEvent<>(
                UUID.randomUUID().toString(),
                eventType,
                Instant.now(),
                VERSION,
                data
        );
    }
}
