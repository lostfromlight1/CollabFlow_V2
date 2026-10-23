package com.collabflow.platform.dto.response;

import com.collabflow.platform.model.OrganizationMember;
import java.time.Instant;
import java.util.UUID;

public record MemberResponse(
        UUID id,
        UUID organizationId,
        UUID userId,
        UUID roleId,
        String roleName,
        Instant joinedAt,
        String name,
        String email
) {
    public static MemberResponse from(OrganizationMember m) {
        return from(m, null, null);
    }

    public static MemberResponse from(OrganizationMember m, String name, String email) {
        return new MemberResponse(
                m.getId(),
                m.getOrganizationId(),
                m.getUserId(),
                m.getRole().getId(),
                m.getRole().getName(),
                m.getJoinedAt(),
                name,
                email
        );
    }
}
