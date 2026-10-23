package com.collabflow.task.event;

import java.util.UUID;

public record TaskUpdatedPayload(UUID taskId, UUID projectId, String name) {}
