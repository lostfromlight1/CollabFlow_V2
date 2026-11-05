package com.collabflow.platform.event;

import java.util.UUID;

/** Soft delete: the organization row stays with {@code is_active = false}. */
public record OrganizationDeletedPayload(UUID organizationId, UUID actorId) {}
