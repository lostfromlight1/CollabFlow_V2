package com.collabflow.user.event;

/**
 * Spring application event published inside the create-user transaction.
 * A listener with {@code AFTER_COMMIT} turns this into a Kafka message.
 */
public record UserCreatedDomainEvent(UserCreatedPayload payload) {}
