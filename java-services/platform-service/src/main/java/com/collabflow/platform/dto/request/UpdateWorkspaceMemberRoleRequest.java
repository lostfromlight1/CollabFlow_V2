package com.collabflow.platform.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record UpdateWorkspaceMemberRoleRequest(
        @NotNull UUID roleId
) {}
