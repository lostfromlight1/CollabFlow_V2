package com.collabflow.task.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "task_types")
public class TaskType {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 50)
    private String icon;

    @Column(nullable = false, length = 7)
    private String color;

    @Column(name = "workspace_id")
    private UUID workspaceId;

    @Column(name = "is_system", nullable = false)
    private boolean system;

    @Column(name = "default_status_id")
    private UUID defaultStatusId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getIcon() {
        return icon;
    }

    public String getColor() {
        return color;
    }

    public UUID getWorkspaceId() {
        return workspaceId;
    }

    public boolean isSystem() {
        return system;
    }

    public UUID getDefaultStatusId() {
        return defaultStatusId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
