package com.collabflow.platform.dto.response;

import com.collabflow.platform.model.Organization;
import java.time.Instant;
import java.util.UUID;

public record OrganizationResponse(
        UUID id,
        String name,
        String description,
        String logoUrl,
        UUID ownerId,
        String slug,
        String plan,
        Integer maxMembers,
        Integer maxWorkspaces,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
    public static OrganizationResponse from(Organization o) {
        return new OrganizationResponse(
                o.getId(),
                o.getName(),
                o.getDescription(),
                o.getLogoUrl(),
                o.getOwnerId(),
                o.getSlug(),
                o.getPlan(),
                o.getMaxMembers(),
                o.getMaxWorkspaces(),
                o.isActive(),
                o.getCreatedAt(),
                o.getUpdatedAt()
        );
    }
}
