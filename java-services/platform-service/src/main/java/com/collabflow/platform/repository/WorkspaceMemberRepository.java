package com.collabflow.platform.repository;

import com.collabflow.platform.model.WorkspaceMember;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WorkspaceMemberRepository extends JpaRepository<WorkspaceMember, UUID> {

    List<WorkspaceMember> findByWorkspaceIdOrderByJoinedAtAsc(UUID workspaceId);

    Optional<WorkspaceMember> findByWorkspaceIdAndUserId(UUID workspaceId, UUID userId);

    Optional<WorkspaceMember> findByIdAndWorkspaceId(UUID id, UUID workspaceId);

    boolean existsByWorkspaceIdAndUserId(UUID workspaceId, UUID userId);

    @Query(value = """
            SELECT COUNT(*)
            FROM workspace_members m
            JOIN role_permissions rp ON rp.role_id = m.role_id
            JOIN permissions p ON p.id = rp.permission_id
            WHERE m.workspace_id = :workspaceId
              AND m.user_id = :userId
              AND p.name = :permission
            """, nativeQuery = true)
    long countWithPermission(
            @Param("workspaceId") UUID workspaceId,
            @Param("userId") UUID userId,
            @Param("permission") String permission
    );
}
