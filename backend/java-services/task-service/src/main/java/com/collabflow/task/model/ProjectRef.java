package com.collabflow.task.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

/** Read-only projection of platform projects (shared DB MVP). */
@Entity
@Table(name = "projects")
public class ProjectRef {

    @Id
    private UUID id;

    @Column(name = "workspace_id", nullable = false)
    private UUID workspaceId;

    @Column(name = "workflow_id")
    private UUID workflowId;

    @Column(nullable = false, length = 20)
    private String status;

    public UUID getId() {
        return id;
    }

    public UUID getWorkspaceId() {
        return workspaceId;
    }

    public UUID getWorkflowId() {
        return workflowId;
    }

    public String getStatus() {
        return status;
    }
}
