package com.collabflow.task.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

public record AssignTaskRequest(
        @Schema(description = "User id; null clears assignee. Must be a project member when set.")
        UUID assigneeId
) {}
