package com.collabflow.task.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

/** Read-only projection of platform workspaces (shared DB MVP). */
@Entity
@Table(name = "workspaces")
public class WorkspaceRef {

    @Id
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    public UUID getId() {
        return id;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public boolean isActive() {
        return active;
    }
}
