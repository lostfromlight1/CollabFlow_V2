package com.collabflow.platform.service;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Read-only name/email for member tables. Users live in the auth schema;
 * platform must not own that table, so this is a lookup, not a User aggregate.
 */
@Service
public class UserDisplayLookup {

    public record UserDisplay(String name, String email) {}

    private final NamedParameterJdbcTemplate jdbc;

    public UserDisplayLookup(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public UserDisplay find(UUID userId) {
        if (userId == null) {
            return null;
        }
        return findByIds(List.of(userId)).get(userId);
    }

    public Map<UUID, UserDisplay> findByIds(Collection<UUID> ids) {
        List<UUID> unique = ids.stream().filter(Objects::nonNull).distinct().toList();
        if (unique.isEmpty()) {
            return Map.of();
        }
        Map<UUID, UserDisplay> out = new HashMap<>();
        jdbc.query(
                "SELECT id, name, email FROM users WHERE id IN (:ids)",
                new MapSqlParameterSource("ids", unique),
                rs -> {
                    out.put(
                            rs.getObject("id", UUID.class),
                            new UserDisplay(rs.getString("name"), rs.getString("email"))
                    );
                }
        );
        return out;
    }
}
