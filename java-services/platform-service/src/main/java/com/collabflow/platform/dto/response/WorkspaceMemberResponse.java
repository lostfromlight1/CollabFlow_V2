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
        Instant joinedAt
) {
    public static WorkspaceMemberResponse from(WorkspaceMember m) {
        return new WorkspaceMemberResponse(
                m.getId(),
                m.getWorkspaceId(),
                m.getUserId(),
                m.getRole().getId(),
                m.getRole().getName(),
                m.getJoinedAt()
        );
    }
}
