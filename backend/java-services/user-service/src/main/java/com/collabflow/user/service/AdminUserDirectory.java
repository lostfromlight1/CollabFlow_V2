package com.collabflow.user.service;

import com.collabflow.common.api.PageResponse;
import com.collabflow.common.exception.BusinessException;
import com.collabflow.common.exception.ErrorCode;
import com.collabflow.user.dto.response.AdminUserResponse;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class AdminUserDirectory {

    static final int EXPORT_CAP = 5000;

    private static final String COLUMNS = """
            u.id, u.email, u.name, u.avatar_url, u.is_email_verified, u.is_active,
            u.is_master_admin, u.created_at, u.last_login_at, u.deleted_at
            """;

    private final NamedParameterJdbcTemplate jdbc;

    public AdminUserDirectory(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public PageResponse<AdminUserResponse> list(
            String q,
            Boolean active,
            Boolean masterAdmin,
            Boolean emailVerified,
            UUID organizationId,
            Instant createdFrom,
            Instant createdTo,
            String deleted,
            int page,
            int size,
            String sort
    ) {
        AdminUserSql.Built built = AdminUserSql.build(
                q, active, masterAdmin, emailVerified, organizationId, createdFrom, createdTo, deleted, sort
        );
        int p = AdminUserSql.page(page);
        int s = AdminUserSql.size(size);
        long total = count(built);
        MapSqlParameterSource params = params(built);
        params.addValue("limit", s);
        params.addValue("offset", p * s);
        List<AdminUserResponse> rows = jdbc.query(
                "SELECT " + COLUMNS + " FROM users u" + built.where() + " ORDER BY " + built.orderBy()
                        + " LIMIT :limit OFFSET :offset",
                params,
                (rs, rowNum) -> map(rs)
        );
        return PageResponse.of(rows, p, s, total);
    }

    public String exportCsv(
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
        AdminUserSql.Built built = AdminUserSql.build(
                q, active, masterAdmin, emailVerified, organizationId, createdFrom, createdTo, deleted, sort
        );
        long total = count(built);
        if (total > EXPORT_CAP) {
            throw new BusinessException(
                    ErrorCode.BAD_REQUEST,
                    "Narrow the filters; " + total + " rows exceed " + EXPORT_CAP
            );
        }
        List<AdminUserResponse> rows = jdbc.query(
                "SELECT " + COLUMNS + " FROM users u" + built.where() + " ORDER BY " + built.orderBy(),
                params(built),
                (rs, rowNum) -> map(rs)
        );
        StringBuilder csv = new StringBuilder();
        csv.append("id,email,name,active,masterAdmin,emailVerified,createdAt,lastLoginAt,deletedAt\n");
        for (AdminUserResponse row : rows) {
            csv.append(cell(row.id().toString())).append(',')
                    .append(cell(row.email())).append(',')
                    .append(cell(row.name())).append(',')
                    .append(row.active()).append(',')
                    .append(row.masterAdmin()).append(',')
                    .append(row.emailVerified()).append(',')
                    .append(cell(text(row.createdAt()))).append(',')
                    .append(cell(text(row.lastLoginAt()))).append(',')
                    .append(cell(text(row.deletedAt())))
                    .append('\n');
        }
        return csv.toString();
    }

    private long count(AdminUserSql.Built built) {
        Long total = jdbc.queryForObject(
                "SELECT COUNT(*) FROM users u" + built.where(),
                params(built),
                Long.class
        );
        return total == null ? 0 : total;
    }

    private static MapSqlParameterSource params(AdminUserSql.Built built) {
        MapSqlParameterSource source = new MapSqlParameterSource();
        built.params().forEach(source::addValue);
        return source;
    }

    private static AdminUserResponse map(ResultSet rs) throws SQLException {
        return new AdminUserResponse(
                rs.getObject("id", UUID.class),
                rs.getString("email"),
                rs.getString("name"),
                rs.getString("avatar_url"),
                rs.getBoolean("is_email_verified"),
                rs.getBoolean("is_active"),
                rs.getBoolean("is_master_admin"),
                instant(rs, "created_at"),
                instant(rs, "last_login_at"),
                instant(rs, "deleted_at")
        );
    }

    private static Instant instant(ResultSet rs, String column) throws SQLException {
        Timestamp ts = rs.getTimestamp(column);
        return ts == null ? null : ts.toInstant();
    }

    private static String text(Instant value) {
        return value == null ? "" : value.toString();
    }

    static String cell(String value) {
        if (value == null) {
            return "";
        }
        String escaped = value.replace("\"", "\"\"");
        if (escaped.contains(",") || escaped.contains("\"") || escaped.contains("\n")) {
            return "\"" + escaped + "\"";
        }
        return escaped;
    }
}
