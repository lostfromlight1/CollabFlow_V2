package com.collabflow.platform.dto.response;

import com.collabflow.platform.model.WorkspaceMember;
import java.time.Instant;
import java.util.UUID;

public record WorkspaceMemberResponse(
        UUID id,
        UUID workspaceId,
        UUID userId,
        UUID roleId,
        String roleName,
        Instant joinedAt,
        String name,
        String email
) {
    public static WorkspaceMemberResponse from(WorkspaceMember m) {
        return from(m, null, null);
    }

    public static WorkspaceMemberResponse from(WorkspaceMember m, String name, String email) {
        return new WorkspaceMemberResponse(
                m.getId(),
                m.getWorkspaceId(),
                m.getUserId(),
                m.getRole().getId(),
                m.getRole().getName(),
                m.getJoinedAt(),
                name,
                email
        );
    }
}
