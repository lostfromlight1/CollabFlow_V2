package com.collabflow.platform.service;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/** Writes admin_audit_events in the current platform transaction. */
@Service
public class AdminAuditWriter {

    private final NamedParameterJdbcTemplate jdbc;

    public AdminAuditWriter(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void write(UUID actorId, String action, String targetType, UUID targetId, String metadataJson) {
        String ip = null;
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            ip = attrs.getRequest().getRemoteAddr();
        }
        jdbc.update(
                """
                INSERT INTO admin_audit_events
                    (id, actor_user_id, action, target_type, target_id, metadata, ip_address, created_at)
                VALUES
                    (:id, :actor, :action, :targetType, :targetId, CAST(:metadata AS jsonb), :ip, :createdAt)
                """,
                new MapSqlParameterSource()
                        .addValue("id", UUID.randomUUID())
                        .addValue("actor", actorId)
                        .addValue("action", action)
                        .addValue("targetType", targetType)
                        .addValue("targetId", targetId)
                        .addValue("metadata", metadataJson == null ? "{}" : metadataJson)
                        .addValue("ip", ip)
                        .addValue("createdAt", Timestamp.from(Instant.now()))
        );
    }
}
