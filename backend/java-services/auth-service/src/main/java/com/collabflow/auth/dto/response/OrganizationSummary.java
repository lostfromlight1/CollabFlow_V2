package com.collabflow.auth.dto.response;

import java.util.UUID;

public record OrganizationSummary(
        UUID id,
        String name,
        String slug,
        UUID roleId
) {}
