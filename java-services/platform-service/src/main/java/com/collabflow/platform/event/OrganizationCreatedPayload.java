package com.collabflow.platform.event;

import java.util.UUID;

public record OrganizationCreatedPayload(UUID organizationId, String name, String slug, UUID ownerId) {}
