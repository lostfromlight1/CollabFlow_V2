package com.collabflow.platform.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AdminStatsResponse(
        long usersTotal,
        long usersActive,
        long usersInactive,
        long usersDeleted,
        long masterAdmins,
        long verifiedUsers,
        long signupsLast7Days,
        long signupsLast30Days,
        long activeUsersLast7Days,
        long orgsTotal,
        long orgsActive,
        long orgsInactive,
        List<SignupDay> signupsByDay,
        List<AdminAuditItem> recentAudit
) {
    public record SignupDay(String date, long count) {}
}
