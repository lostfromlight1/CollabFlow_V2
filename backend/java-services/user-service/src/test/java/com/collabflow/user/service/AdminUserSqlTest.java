package com.collabflow.user.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AdminUserSqlTest {

    @Test
    void hidesDeletedAndFiltersByOrganization() {
        UUID orgId = UUID.randomUUID();
        AdminUserSql.Built built = AdminUserSql.build(
                "ka_ung",
                true,
                null,
                null,
                orgId,
                Instant.parse("2026-01-01T00:00:00Z"),
                null,
                "false",
                "email,asc"
        );

        assertThat(built.where()).contains("u.deleted_at IS NULL");
        assertThat(built.where()).contains("organization_members");
        assertThat(built.where()).contains("u.is_active = :active");
        assertThat(built.params()).containsEntry("organizationId", orgId);
        assertThat(built.params().get("q")).isEqualTo("%ka!_ung%");
        assertThat(built.orderBy()).isEqualTo("u.email ASC");
    }

    @Test
    void unknownSortFallsBackToCreatedAt() {
        AdminUserSql.Built built = AdminUserSql.build(null, null, null, null, null, null, null, "any", "password,asc");
        assertThat(built.where()).doesNotContain("deleted_at");
        assertThat(built.orderBy()).isEqualTo("u.created_at DESC");
    }
}
