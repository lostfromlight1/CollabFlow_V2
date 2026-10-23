package com.collabflow.user.service;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Allowlisted WHERE/ORDER for the master-admin user directory. */
public final class AdminUserSql {

    public record Built(String where, Map<String, Object> params, String orderBy) {}

    private AdminUserSql() {}

    public static Built build(
            String q,
            Boolean active,
            Boolean masterAdmin,
            Boolean emailVerified,
            UUID organizationId,
            Instant createdFrom,
            Instant createdTo,
            String deleted,
            String sort
    ) {
        StringBuilder where = new StringBuilder(" WHERE 1=1");
        Map<String, Object> params = new HashMap<>();
        String deletedMode = deleted == null || deleted.isBlank() ? "false" : deleted;
        if ("true".equalsIgnoreCase(deletedMode)) {
            where.append(" AND u.deleted_at IS NOT NULL");
        } else if (!"any".equalsIgnoreCase(deletedMode)) {
            where.append(" AND u.deleted_at IS NULL");
        }
        if (q != null && !q.isBlank()) {
            where.append(" AND (u.name ILIKE :q ESCAPE '!' OR u.email ILIKE :q ESCAPE '!')");
            params.put("q", like(q));
        }
        if (active != null) {
            where.append(" AND u.is_active = :active");
            params.put("active", active);
        }
        if (masterAdmin != null) {
            where.append(" AND u.is_master_admin = :masterAdmin");
            params.put("masterAdmin", masterAdmin);
        }
        if (emailVerified != null) {
            where.append(" AND u.is_email_verified = :emailVerified");
            params.put("emailVerified", emailVerified);
        }
        if (organizationId != null) {
            where.append("""
                     AND EXISTS (
                       SELECT 1 FROM organization_members m
                       WHERE m.user_id = u.id AND m.organization_id = :organizationId
                     )""");
            params.put("organizationId", organizationId);
        }
        if (createdFrom != null) {
            where.append(" AND u.created_at >= :createdFrom");
            params.put("createdFrom", Timestamp.from(createdFrom));
        }
        if (createdTo != null) {
            where.append(" AND u.created_at <= :createdTo");
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
            case "name" -> "u.name " + (asc ? "ASC" : "DESC");
            case "email" -> "u.email " + (asc ? "ASC" : "DESC");
            case "createdAt" -> "u.created_at " + (asc ? "ASC" : "DESC");
            default -> "u.created_at DESC";
        };
    }

    public static int page(int page) {
        return Math.max(page, 0);
    }

    public static int size(int size) {
        return Math.min(Math.max(size, 1), 100);
    }
}
