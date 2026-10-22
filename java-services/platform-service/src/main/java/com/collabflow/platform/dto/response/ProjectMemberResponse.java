package com.collabflow.platform.dto.response;

import com.collabflow.platform.model.ProjectMember;
import java.time.Instant;
import java.util.UUID;

public record ProjectMemberResponse(
        UUID id,
        UUID projectId,
        UUID userId,
        UUID roleId,
        String roleName,
        Instant joinedAt
) {
    public static ProjectMemberResponse from(ProjectMember m) {
        return new ProjectMemberResponse(
                m.getId(),
                m.getProjectId(),
                m.getUserId(),
                m.getRole().getId(),
                m.getRole().getName(),
                m.getJoinedAt()
        );
    }
}
