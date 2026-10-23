package com.collabflow.platform.service;

import com.collabflow.common.api.PageResponse;
import com.collabflow.common.exception.BusinessException;
import com.collabflow.common.exception.ConflictException;
import com.collabflow.common.exception.ErrorCode;
import com.collabflow.common.exception.ForbiddenException;
import com.collabflow.common.exception.ResourceNotFoundException;
import com.collabflow.platform.dto.request.AnnouncementRequest;
import com.collabflow.platform.dto.request.FeatureFlagRequest;
import com.collabflow.platform.dto.response.AdminAuditItem;
import com.collabflow.platform.dto.response.AdminOrganizationDetail;
import com.collabflow.platform.dto.response.AdminOrganizationDetail.AdminWorkspaceSummary;
import com.collabflow.platform.dto.response.AdminOrganizationSummary;
import com.collabflow.platform.dto.response.AdminStatsResponse;
import com.collabflow.platform.dto.response.AdminStatsResponse.SignupDay;
import com.collabflow.platform.dto.response.AdminUserMembership;
import com.collabflow.platform.dto.response.AnnouncementResponse;
import com.collabflow.platform.dto.response.FeatureFlagResponse;
import com.collabflow.platform.dto.response.MemberResponse;
import com.collabflow.platform.model.Announcement;
import com.collabflow.platform.model.FeatureFlag;
import com.collabflow.platform.model.Organization;
import com.collabflow.platform.model.OrganizationMember;
import com.collabflow.platform.repository.AnnouncementRepository;
import com.collabflow.platform.repository.FeatureFlagRepository;
import com.collabflow.platform.repository.OrganizationMemberRepository;
import com.collabflow.platform.repository.OrganizationRepository;
import com.collabflow.platform.repository.UserRefRepository;
import com.collabflow.platform.service.UserDisplayLookup.UserDisplay;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminConsoleService {

    private static final Logger log = LoggerFactory.getLogger(AdminConsoleService.class);
    private static final int EXPORT_CAP = 5000;

    private static final String ORG_COLUMNS = """
            o.id, o.name, o.description, o.slug, o.plan, o.is_active, o.created_at,
            o.owner_id, u.name AS owner_name, u.email AS owner_email
            """;

    private final NamedParameterJdbcTemplate jdbc;
    private final OrganizationRepository organizationRepository;
    private final OrganizationMemberRepository memberRepository;
    private final UserRefRepository userRefRepository;
    private final UserDisplayLookup userDisplayLookup;
    private final AnnouncementRepository announcementRepository;
    private final FeatureFlagRepository featureFlagRepository;
    private final AdminAuditWriter auditWriter;

    public AdminConsoleService(
            NamedParameterJdbcTemplate jdbc,
            OrganizationRepository organizationRepository,
            OrganizationMemberRepository memberRepository,
            UserRefRepository userRefRepository,
            UserDisplayLookup userDisplayLookup,
            AnnouncementRepository announcementRepository,
            FeatureFlagRepository featureFlagRepository,
            AdminAuditWriter auditWriter
    ) {
        this.jdbc = jdbc;
        this.organizationRepository = organizationRepository;
        this.memberRepository = memberRepository;
        this.userRefRepository = userRefRepository;
        this.userDisplayLookup = userDisplayLookup;
        this.announcementRepository = announcementRepository;
        this.featureFlagRepository = featureFlagRepository;
        this.auditWriter = auditWriter;
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminOrganizationSummary> listOrganizations(
            UUID adminId, String q, Boolean active, String plan,
            Instant createdFrom, Instant createdTo, int page, int size, String sort
    ) {
        requireMaster(adminId);
        AdminOrgSql.Built built = AdminOrgSql.build(q, active, plan, createdFrom, createdTo, sort);
        int p = Math.max(page, 0);
        int s = Math.min(Math.max(size, 1), 100);
        long total = countOrgs(built);
        MapSqlParameterSource params = params(built);
        params.addValue("limit", s);
        params.addValue("offset", p * s);
        List<AdminOrganizationSummary> rows = jdbc.query(
                "SELECT " + ORG_COLUMNS + " FROM organizations o LEFT JOIN users u ON u.id = o.owner_id"
                        + built.where() + " ORDER BY " + built.orderBy() + " LIMIT :limit OFFSET :offset",
                params,
                (rs, n) -> mapOrg(rs)
        );
        return PageResponse.of(rows, p, s, total);
    }

    @Transactional(readOnly = true)
    public String exportOrganizations(
            UUID adminId, String q, Boolean active, String plan,
            Instant createdFrom, Instant createdTo, String sort
    ) {
        requireMaster(adminId);
        AdminOrgSql.Built built = AdminOrgSql.build(q, active, plan, createdFrom, createdTo, sort);
        long total = countOrgs(built);
        if (total > EXPORT_CAP) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Narrow the filters; " + total + " rows exceed " + EXPORT_CAP);
        }
        List<AdminOrganizationSummary> rows = jdbc.query(
                "SELECT " + ORG_COLUMNS + " FROM organizations o LEFT JOIN users u ON u.id = o.owner_id"
                        + built.where() + " ORDER BY " + built.orderBy(),
                params(built),
                (rs, n) -> mapOrg(rs)
        );
        StringBuilder csv = new StringBuilder();
        csv.append("id,name,slug,plan,active,ownerId,ownerName,ownerEmail,createdAt\n");
        for (AdminOrganizationSummary row : rows) {
            csv.append(row.id()).append(',')
                    .append(csvCell(row.name())).append(',')
                    .append(csvCell(row.slug())).append(',')
                    .append(csvCell(row.plan())).append(',')
                    .append(row.active()).append(',')
                    .append(row.ownerId()).append(',')
                    .append(csvCell(row.ownerName())).append(',')
                    .append(csvCell(row.ownerEmail())).append(',')
                    .append(row.createdAt() == null ? "" : row.createdAt())
                    .append('\n');
        }
        return csv.toString();
    }

    @Transactional(readOnly = true)
    public AdminOrganizationDetail getOrganization(UUID adminId, UUID organizationId) {
        requireMaster(adminId);
        AdminOrganizationSummary summary = jdbc.query(
                "SELECT " + ORG_COLUMNS + " FROM organizations o LEFT JOIN users u ON u.id = o.owner_id WHERE o.id = :id",
                new MapSqlParameterSource("id", organizationId),
                (rs, n) -> mapOrg(rs)
        ).stream().findFirst().orElseThrow(() -> new ResourceNotFoundException("Organization not found"));

        Long members = jdbc.queryForObject(
                "SELECT COUNT(*) FROM organization_members WHERE organization_id = :id",
                new MapSqlParameterSource("id", organizationId),
                Long.class
        );
        Long workspaces = jdbc.queryForObject(
                "SELECT COUNT(*) FROM workspaces WHERE organization_id = :id",
                new MapSqlParameterSource("id", organizationId),
                Long.class
        );
        Long projects = jdbc.queryForObject(
                """
                SELECT COUNT(*) FROM projects p
                JOIN workspaces w ON w.id = p.workspace_id
                WHERE w.organization_id = :id
                """,
                new MapSqlParameterSource("id", organizationId),
                Long.class
        );
        List<AdminWorkspaceSummary> workspaceRows = jdbc.query(
                """
                SELECT w.id, w.name, w.is_active,
                       (SELECT COUNT(*) FROM projects p WHERE p.workspace_id = w.id) AS project_count
                FROM workspaces w
                WHERE w.organization_id = :id
                ORDER BY w.name
                LIMIT 50
                """,
                new MapSqlParameterSource("id", organizationId),
                (rs, n) -> new AdminWorkspaceSummary(
                        rs.getObject("id", UUID.class),
                        rs.getString("name"),
                        rs.getBoolean("is_active"),
                        rs.getLong("project_count")
                )
        );
        return new AdminOrganizationDetail(
                summary,
                members == null ? 0 : members,
                workspaces == null ? 0 : workspaces,
                projects == null ? 0 : projects,
                workspaceRows
        );
    }

    @Transactional(readOnly = true)
    public List<AdminUserMembership> membershipsForUser(UUID adminId, UUID userId) {
        requireMaster(adminId);
        return jdbc.query(
                """
                SELECT o.id, o.name, o.slug, o.is_active, r.id AS role_id, r.name AS role_name
                FROM organization_members m
                JOIN organizations o ON o.id = m.organization_id
                JOIN roles r ON r.id = m.role_id
                WHERE m.user_id = :userId
                ORDER BY o.name
                """,
                new MapSqlParameterSource("userId", userId),
                (rs, n) -> new AdminUserMembership(
                        rs.getObject("id", UUID.class),
                        rs.getString("name"),
                        rs.getString("slug"),
                        rs.getBoolean("is_active"),
                        rs.getObject("role_id", UUID.class),
                        rs.getString("role_name")
                )
        );
    }

    @Transactional(readOnly = true)
    public PageResponse<MemberResponse> listMembers(UUID adminId, UUID organizationId, int page, int size) {
        requireMaster(adminId);
        requireOrg(organizationId);
        int p = Math.max(page, 0);
        int s = Math.min(Math.max(size, 1), 100);
        var result = memberRepository.findByOrganizationId(organizationId, PageRequest.of(p, s));
        List<UUID> userIds = result.getContent().stream().map(OrganizationMember::getUserId).toList();
        Map<UUID, UserDisplay> names = userDisplayLookup.findByIds(userIds);
        List<MemberResponse> rows = result.getContent().stream()
                .map(member -> {
                    UserDisplay display = names.get(member.getUserId());
                    return MemberResponse.from(
                            member,
                            display == null ? null : display.name(),
                            display == null ? null : display.email()
                    );
                })
                .toList();
        return PageResponse.of(rows, p, s, result.getTotalElements());
    }

    @Transactional
    public void deactivateOrganization(UUID adminId, UUID organizationId) {
        requireMaster(adminId);
        Organization org = requireOrg(organizationId);
        if (!org.isActive()) {
            throw new ConflictException("Organization is already deactivated");
        }
        org.setActive(false);
        auditWriter.write(adminId, "ORG_DEACTIVATED", "ORGANIZATION", organizationId, null);
        log.info("Admin deactivated orgId={} by={}", organizationId, adminId);
    }

    @Transactional
    public void activateOrganization(UUID adminId, UUID organizationId) {
        requireMaster(adminId);
        Organization org = requireOrg(organizationId);
        if (org.isActive()) {
            throw new ConflictException("Organization is already active");
        }
        org.setActive(true);
        auditWriter.write(adminId, "ORG_ACTIVATED", "ORGANIZATION", organizationId, null);
        log.info("Admin activated orgId={} by={}", organizationId, adminId);
    }

    @Transactional(readOnly = true)
    public AdminStatsResponse stats(UUID adminId) {
        requireMaster(adminId);
        Instant now = Instant.now();
        MapSqlParameterSource users = new MapSqlParameterSource()
                .addValue("d7", Timestamp.from(now.minus(7, ChronoUnit.DAYS)))
                .addValue("d30", Timestamp.from(now.minus(30, ChronoUnit.DAYS)));
        record Counts(
                long total, long active, long inactive, long deleted, long masters,
                long verified, long signups7, long signups30, long active7
        ) {}
        Counts userCounts = jdbc.queryForObject(
                """
                SELECT
                  COUNT(*) AS total,
                  COUNT(*) FILTER (WHERE is_active AND deleted_at IS NULL) AS active,
                  COUNT(*) FILTER (WHERE NOT is_active AND deleted_at IS NULL) AS inactive,
                  COUNT(*) FILTER (WHERE deleted_at IS NOT NULL) AS deleted,
                  COUNT(*) FILTER (WHERE is_master_admin AND deleted_at IS NULL) AS masters,
                  COUNT(*) FILTER (WHERE is_email_verified AND deleted_at IS NULL) AS verified,
                  COUNT(*) FILTER (WHERE created_at >= :d7) AS signups7,
                  COUNT(*) FILTER (WHERE created_at >= :d30) AS signups30,
                  COUNT(*) FILTER (WHERE last_login_at >= :d7 AND deleted_at IS NULL) AS active7
                FROM users
                """,
                users,
                (rs, n) -> new Counts(
                        rs.getLong("total"), rs.getLong("active"), rs.getLong("inactive"),
                        rs.getLong("deleted"), rs.getLong("masters"), rs.getLong("verified"),
                        rs.getLong("signups7"), rs.getLong("signups30"), rs.getLong("active7")
                )
        );
        long orgsTotal = count("SELECT COUNT(*) FROM organizations");
        long orgsActive = count("SELECT COUNT(*) FROM organizations WHERE is_active");
        List<SignupDay> days = jdbc.query(
                """
                SELECT to_char(date_trunc('day', created_at), 'YYYY-MM-DD') AS day, COUNT(*) AS count
                FROM users
                WHERE created_at >= :d30
                GROUP BY 1
                ORDER BY 1
                """,
                new MapSqlParameterSource("d30", Timestamp.from(now.minus(30, ChronoUnit.DAYS))),
                (rs, n) -> new SignupDay(rs.getString("day"), rs.getLong("count"))
        );
        return new AdminStatsResponse(
                userCounts.total(), userCounts.active(), userCounts.inactive(), userCounts.deleted(),
                userCounts.masters(), userCounts.verified(), userCounts.signups7(), userCounts.signups30(),
                userCounts.active7(), orgsTotal, orgsActive, orgsTotal - orgsActive,
                days, recentAudit(10)
        );
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminAuditItem> audit(
            UUID adminId, UUID actorUserId, String action, String targetType, UUID targetId,
            Instant createdFrom, Instant createdTo, int page, int size
    ) {
        requireMaster(adminId);
        StringBuilder where = new StringBuilder(" WHERE 1=1");
        MapSqlParameterSource params = new MapSqlParameterSource();
        if (actorUserId != null) {
            where.append(" AND actor_user_id = :actor");
            params.addValue("actor", actorUserId);
        }
        if (action != null && !action.isBlank()) {
            where.append(" AND action = :action");
            params.addValue("action", action.trim());
        }
        if (targetType != null && !targetType.isBlank()) {
            where.append(" AND target_type = :targetType");
            params.addValue("targetType", targetType.trim());
        }
        if (targetId != null) {
            where.append(" AND target_id = :targetId");
            params.addValue("targetId", targetId);
        }
        if (createdFrom != null) {
            where.append(" AND created_at >= :createdFrom");
            params.addValue("createdFrom", Timestamp.from(createdFrom));
        }
        if (createdTo != null) {
            where.append(" AND created_at <= :createdTo");
            params.addValue("createdTo", Timestamp.from(createdTo));
        }
        int p = Math.max(page, 0);
        int s = Math.min(Math.max(size, 1), 100);
        Long total = jdbc.queryForObject("SELECT COUNT(*) FROM admin_audit_events" + where, params, Long.class);
        params.addValue("limit", s);
        params.addValue("offset", p * s);
        List<AdminAuditItem> rows = jdbc.query(
                "SELECT id, actor_user_id, action, target_type, target_id, metadata::text AS metadata, ip_address, created_at"
                        + " FROM admin_audit_events" + where + " ORDER BY created_at DESC LIMIT :limit OFFSET :offset",
                params,
                (rs, n) -> mapAudit(rs)
        );
        return PageResponse.of(rows, p, s, total == null ? 0 : total);
    }

    @Transactional
    public AnnouncementResponse createAnnouncement(UUID adminId, AnnouncementRequest request) {
        requireMaster(adminId);
        Announcement row = new Announcement();
        apply(row, request);
        row.setCreatedBy(adminId);
        announcementRepository.save(row);
        auditWriter.write(adminId, "ANNOUNCEMENT_CREATED", "ANNOUNCEMENT", row.getId(), null);
        return toAnnouncement(row);
    }

    @Transactional(readOnly = true)
    public List<AnnouncementResponse> listAnnouncements(UUID adminId) {
        requireMaster(adminId);
        return announcementRepository.findAllByOrderByCreatedAtDesc().stream().map(this::toAnnouncement).toList();
    }

    @Transactional
    public AnnouncementResponse updateAnnouncement(UUID adminId, UUID id, AnnouncementRequest request) {
        requireMaster(adminId);
        Announcement row = announcementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Announcement not found"));
        apply(row, request);
        auditWriter.write(adminId, "ANNOUNCEMENT_UPDATED", "ANNOUNCEMENT", id, null);
        return toAnnouncement(row);
    }

    @Transactional
    public void deleteAnnouncement(UUID adminId, UUID id) {
        requireMaster(adminId);
        if (!announcementRepository.existsById(id)) {
            throw new ResourceNotFoundException("Announcement not found");
        }
        announcementRepository.deleteById(id);
        auditWriter.write(adminId, "ANNOUNCEMENT_DELETED", "ANNOUNCEMENT", id, null);
    }

    @Transactional(readOnly = true)
    public AnnouncementResponse activeAnnouncement() {
        Instant now = Instant.now();
        return announcementRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(Announcement::isActive)
                .filter(row -> row.getStartsAt() == null || !row.getStartsAt().isAfter(now))
                .filter(row -> row.getEndsAt() == null || row.getEndsAt().isAfter(now))
                .findFirst()
                .map(this::toAnnouncement)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public List<FeatureFlagResponse> listFlags(UUID adminId) {
        requireMaster(adminId);
        return featureFlagRepository.findAllByOrderByKeyAsc().stream().map(this::toFlag).toList();
    }

    @Transactional(readOnly = true)
    public List<FeatureFlagResponse> listPublicFlags() {
        return featureFlagRepository.findAllByOrderByKeyAsc().stream().map(this::toFlag).toList();
    }

    @Transactional
    public FeatureFlagResponse upsertFlag(UUID adminId, String key, FeatureFlagRequest request) {
        requireMaster(adminId);
        if (key == null || !key.matches("^[a-z0-9]+(?:-[a-z0-9]+)*$")) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Flag key must be lowercase alphanumeric with hyphens");
        }
        FeatureFlag row = featureFlagRepository.findById(key).orElseGet(() -> {
            FeatureFlag created = new FeatureFlag();
            created.setKey(key);
            return created;
        });
        row.setEnabled(request.enabled());
        row.setDescription(request.description());
        featureFlagRepository.save(row);
        auditWriter.write(adminId, "FEATURE_FLAG_UPSERTED", "FEATURE_FLAG", null, "{\"key\":\"" + key + "\"}");
        return toFlag(row);
    }

    @Transactional
    public void deleteFlag(UUID adminId, String key) {
        requireMaster(adminId);
        if (!featureFlagRepository.existsById(key)) {
            throw new ResourceNotFoundException("Feature flag not found");
        }
        featureFlagRepository.deleteById(key);
        auditWriter.write(adminId, "FEATURE_FLAG_DELETED", "FEATURE_FLAG", null, "{\"key\":\"" + key + "\"}");
    }

    private List<AdminAuditItem> recentAudit(int limit) {
        return jdbc.query(
                """
                SELECT id, actor_user_id, action, target_type, target_id, metadata::text AS metadata, ip_address, created_at
                FROM admin_audit_events
                ORDER BY created_at DESC
                LIMIT :limit
                """,
                new MapSqlParameterSource("limit", limit),
                (rs, n) -> mapAudit(rs)
        );
    }

    private long countOrgs(AdminOrgSql.Built built) {
        Long total = jdbc.queryForObject("SELECT COUNT(*) FROM organizations o" + built.where(), params(built), Long.class);
        return total == null ? 0 : total;
    }

    private long count(String sql) {
        Long total = jdbc.queryForObject(sql, new MapSqlParameterSource(), Long.class);
        return total == null ? 0 : total;
    }

    private void requireMaster(UUID adminId) {
        if (!userRefRepository.existsByIdAndActiveTrueAndMasterAdminTrue(adminId)) {
            throw new ForbiddenException("Master admin required");
        }
    }

    private Organization requireOrg(UUID organizationId) {
        return organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));
    }

    private static MapSqlParameterSource params(AdminOrgSql.Built built) {
        MapSqlParameterSource source = new MapSqlParameterSource();
        built.params().forEach(source::addValue);
        return source;
    }

    private static AdminOrganizationSummary mapOrg(ResultSet rs) throws SQLException {
        Timestamp created = rs.getTimestamp("created_at");
        return new AdminOrganizationSummary(
                rs.getObject("id", UUID.class),
                rs.getString("name"),
                rs.getString("description"),
                rs.getString("slug"),
                rs.getString("plan"),
                rs.getBoolean("is_active"),
                created == null ? null : created.toInstant(),
                rs.getObject("owner_id", UUID.class),
                rs.getString("owner_name"),
                rs.getString("owner_email")
        );
    }

    private static AdminAuditItem mapAudit(ResultSet rs) throws SQLException {
        Timestamp created = rs.getTimestamp("created_at");
        return new AdminAuditItem(
                rs.getObject("id", UUID.class),
                rs.getObject("actor_user_id", UUID.class),
                rs.getString("action"),
                rs.getString("target_type"),
                rs.getObject("target_id", UUID.class),
                rs.getString("metadata"),
                rs.getString("ip_address"),
                created == null ? null : created.toInstant()
        );
    }

    private static void apply(Announcement row, AnnouncementRequest request) {
        row.setTitle(request.title().trim());
        row.setBody(request.body().trim());
        row.setActive(request.active());
        row.setStartsAt(request.startsAt());
        row.setEndsAt(request.endsAt());
    }

    private AnnouncementResponse toAnnouncement(Announcement row) {
        return new AnnouncementResponse(
                row.getId(), row.getTitle(), row.getBody(), row.isActive(),
                row.getStartsAt(), row.getEndsAt(), row.getCreatedBy(), row.getCreatedAt()
        );
    }

    private FeatureFlagResponse toFlag(FeatureFlag row) {
        return new FeatureFlagResponse(row.getKey(), row.isEnabled(), row.getDescription(), row.getUpdatedAt());
    }

    private static String csvCell(String value) {
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
