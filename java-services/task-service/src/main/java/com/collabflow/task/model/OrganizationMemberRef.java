package com.collabflow.task.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

/** Read-only organization_members for org-header checks. */
@Entity
@Table(name = "organization_members")
public class OrganizationMemberRef {

    @Id
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    public UUID getId() {
        return id;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public UUID getUserId() {
        return userId;
    }
}
