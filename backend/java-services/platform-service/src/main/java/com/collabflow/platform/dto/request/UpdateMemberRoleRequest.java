package com.collabflow.platform.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record UpdateMemberRoleRequest(@NotNull UUID roleId) {}
