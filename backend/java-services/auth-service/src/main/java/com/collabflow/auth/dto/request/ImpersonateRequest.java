package com.collabflow.auth.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ImpersonateRequest(@NotNull UUID userId) {}
