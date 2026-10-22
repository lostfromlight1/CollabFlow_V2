package com.collabflow.platform.event;

import java.util.UUID;

public record OrganizationUpdatedPayload(UUID organizationId, String name, String slug) {}
