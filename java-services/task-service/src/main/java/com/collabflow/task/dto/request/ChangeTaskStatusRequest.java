package com.collabflow.task.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ChangeTaskStatusRequest(@NotNull UUID statusId) {}
