package com.collabflow.platform.service;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/** Allowlisted WHERE/ORDER for the master-admin organization directory. */
public final class AdminOrgSql {

    public record Built(String where, Map<String, Object> params, String orderBy) {}

    private AdminOrgSql() {}

    public static Built build(
            String q,
            Boolean active,
            String plan,
            Instant createdFrom,
            Instant createdTo,
            String sort
    ) {
        StringBuilder where = new StringBuilder(" WHERE 1=1");
        Map<String, Object> params = new HashMap<>();
        if (q != null && !q.isBlank()) {
            where.append(" AND (o.name ILIKE :q ESCAPE '!' OR o.slug ILIKE :q ESCAPE '!')");
            params.put("q", like(q));
        }
        if (active != null) {
            where.append(" AND o.is_active = :active");
            params.put("active", active);
        }
        if (plan != null && !plan.isBlank()) {
            where.append(" AND o.plan = :plan");
            params.put("plan", plan.trim());
        }
        if (createdFrom != null) {
            where.append(" AND o.created_at >= :createdFrom");
            params.put("createdFrom", Timestamp.from(createdFrom));
        }
        if (createdTo != null) {
            where.append(" AND o.created_at <= :createdTo");
            params.put("createdTo", Timestamp.from(createdTo));
        }
        return new Built(where.toString(), params, order(sort));
    }

    public static String like(String raw) {
        String escaped = raw.trim()
                .replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_");
        return "%" + escaped + "%";
    }

    public static String order(String sort) {
        String raw = sort == null || sort.isBlank() ? "createdAt,desc" : sort;
        String[] parts = raw.split(",", 2);
        String field = parts[0].trim();
        boolean asc = parts.length > 1 && "asc".equalsIgnoreCase(parts[1].trim());
        return switch (field) {
            case "name" -> "o.name " + (asc ? "ASC" : "DESC");
            case "slug" -> "o.slug " + (asc ? "ASC" : "DESC");
            case "createdAt" -> "o.created_at " + (asc ? "ASC" : "DESC");
            default -> "o.created_at DESC";
        };
    }
}
