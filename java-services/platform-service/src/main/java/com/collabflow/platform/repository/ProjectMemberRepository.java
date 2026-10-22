package com.collabflow.platform.repository;

import com.collabflow.platform.model.ProjectMember;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, UUID> {

    List<ProjectMember> findByProjectIdOrderByJoinedAtAsc(UUID projectId);

    Optional<ProjectMember> findByProjectIdAndUserId(UUID projectId, UUID userId);

    Optional<ProjectMember> findByIdAndProjectId(UUID id, UUID projectId);

    boolean existsByProjectIdAndUserId(UUID projectId, UUID userId);

    @Query(value = """
            SELECT COUNT(*)
            FROM project_members m
            JOIN role_permissions rp ON rp.role_id = m.role_id
            JOIN permissions p ON p.id = rp.permission_id
            WHERE m.project_id = :projectId
              AND m.user_id = :userId
              AND p.name = :permission
            """, nativeQuery = true)
    long countWithPermission(
            @Param("projectId") UUID projectId,
            @Param("userId") UUID userId,
            @Param("permission") String permission
    );
}
