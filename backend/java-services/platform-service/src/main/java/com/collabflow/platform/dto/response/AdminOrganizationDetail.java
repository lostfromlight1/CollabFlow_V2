package com.collabflow.platform.dto.response;

import java.util.List;
import java.util.UUID;

public record AdminOrganizationDetail(
        AdminOrganizationSummary organization,
        long memberCount,
        long workspaceCount,
        long projectCount,
        List<AdminWorkspaceSummary> workspaces
) {
    public record AdminWorkspaceSummary(UUID id, String name, boolean active, long projectCount) {}
}
