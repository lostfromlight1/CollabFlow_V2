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
        Instant joinedAt,
        String name,
        String email
) {
    public static ProjectMemberResponse from(ProjectMember m) {
        return from(m, null, null);
    }

    public static ProjectMemberResponse from(ProjectMember m, String name, String email) {
        return new ProjectMemberResponse(
                m.getId(),
                m.getProjectId(),
                m.getUserId(),
                m.getRole().getId(),
                m.getRole().getName(),
                m.getJoinedAt(),
                name,
                email
        );
    }
}
