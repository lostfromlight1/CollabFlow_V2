package com.collabflow.platform.dto.response;

import com.collabflow.platform.model.Role;
import java.util.UUID;

public record RoleResponse(UUID id, String name, String description, String scope) {
    public static RoleResponse from(Role role) {
        return new RoleResponse(role.getId(), role.getName(), role.getDescription(), role.getScope());
    }
}
