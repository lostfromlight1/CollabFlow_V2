package com.collabflow.auth.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record SelectOrganizationRequest(@NotNull UUID organizationId) {}
