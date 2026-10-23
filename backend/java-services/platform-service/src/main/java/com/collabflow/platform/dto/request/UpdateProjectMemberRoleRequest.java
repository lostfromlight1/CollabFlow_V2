package com.collabflow.platform.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record UpdateProjectMemberRoleRequest(@NotNull UUID roleId) {}
