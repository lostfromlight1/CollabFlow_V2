package com.collabflow.user.event;

/** Spring application event published inside a profile-update transaction. */
public record UserUpdatedDomainEvent(UserUpdatedPayload payload) {}
