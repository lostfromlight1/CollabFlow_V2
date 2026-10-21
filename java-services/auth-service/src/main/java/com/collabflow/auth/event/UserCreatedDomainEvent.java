package com.collabflow.auth.event;

/**
 * Spring application event published inside the register transaction.
 * A listener with {@code AFTER_COMMIT} turns this into a Kafka message —
 * so we never announce a user that rolled back.
 */
public record UserCreatedDomainEvent(UserCreatedPayload payload) {}
