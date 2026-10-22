package com.collabflow.platform.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

/**
 * Read-only projection of auth {@code users} (shared DB MVP).
 * {@code is_master_admin} is owned by auth-service Flyway — platform only reads it.
 */
@Entity
@Table(name = "users")
public class UserRef {

    @Id
    private UUID id;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "is_master_admin", nullable = false)
    private boolean masterAdmin;

    public UUID getId() {
        return id;
    }

    public boolean isActive() {
        return active;
    }

    public boolean isMasterAdmin() {
        return masterAdmin;
    }
}
