package com.collabflow.task.dto.response;

import java.util.List;
import java.util.UUID;

/** Basic MVP analytics: task counts for one project. */
public record TaskStatsResponse(
        UUID projectId,
        long total,
        List<StatusCount> byStatus,
        List<PriorityCount> byPriority
) {
    public record StatusCount(UUID statusId, String name, String category, long count) {}

    public record PriorityCount(String priority, long count) {}
}
