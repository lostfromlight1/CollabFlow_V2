package com.collabflow.platform.repository;

import com.collabflow.platform.model.OrganizationMember;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrganizationMemberRepository extends JpaRepository<OrganizationMember, UUID> {

    List<OrganizationMember> findByOrganizationIdOrderByJoinedAtAsc(UUID organizationId);

    List<OrganizationMember> findByUserIdOrderByJoinedAtAsc(UUID userId);

    Optional<OrganizationMember> findByOrganizationIdAndUserId(UUID organizationId, UUID userId);

    Optional<OrganizationMember> findByIdAndOrganizationId(UUID id, UUID organizationId);

    boolean existsByOrganizationIdAndUserId(UUID organizationId, UUID userId);

    long countByOrganizationId(UUID organizationId);

    @Query(value = """
            SELECT COUNT(*)
            FROM organization_members m
            JOIN role_permissions rp ON rp.role_id = m.role_id
            JOIN permissions p ON p.id = rp.permission_id
            WHERE m.organization_id = :organizationId
              AND m.user_id = :userId
              AND p.name = :permission
            """, nativeQuery = true)
    long countWithPermission(
            @Param("organizationId") UUID organizationId,
            @Param("userId") UUID userId,
            @Param("permission") String permission
    );
}
