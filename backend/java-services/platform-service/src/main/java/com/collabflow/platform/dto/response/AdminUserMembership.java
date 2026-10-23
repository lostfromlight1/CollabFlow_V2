package com.collabflow.platform.dto.response;

import java.util.UUID;

public record AdminUserMembership(
        UUID organizationId,
        String name,
        String slug,
        boolean active,
        UUID roleId,
        String roleName
) {}
