package com.collabflow.platform.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class AdminOrgSqlTest {

    @Test
    void filtersActivePlanAndSearch() {
        AdminOrgSql.Built built = AdminOrgSql.build("acme", false, "FREE", null, null, "slug,asc");
        assertThat(built.where()).contains("o.is_active = :active");
        assertThat(built.where()).contains("o.plan = :plan");
        assertThat(built.where()).contains("o.name ILIKE");
        assertThat(built.params()).containsEntry("active", false);
        assertThat(built.params()).containsEntry("plan", "FREE");
        assertThat(built.orderBy()).isEqualTo("o.slug ASC");
        assertThat(built.params().get("q")).isEqualTo("%acme%");
    }

    @Test
    void unknownSortFallsBack() {
        AdminOrgSql.Built built = AdminOrgSql.build(null, null, null, null, null, "owner,desc");
        assertThat(built.orderBy()).isEqualTo("o.created_at DESC");
        assertThat(UUID.randomUUID()).isNotNull();
    }
}
