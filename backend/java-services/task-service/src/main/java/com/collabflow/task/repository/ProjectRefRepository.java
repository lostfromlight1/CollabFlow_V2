package com.collabflow.task.repository;

import com.collabflow.task.model.ProjectRef;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProjectRefRepository extends JpaRepository<ProjectRef, UUID> {

    Optional<ProjectRef> findByIdAndStatusNot(UUID id, String status);

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

    @Query(value = """
            SELECT COUNT(*)
            FROM project_members m
            WHERE m.project_id = :projectId
              AND m.user_id = :userId
            """, nativeQuery = true)
    long countMembers(@Param("projectId") UUID projectId, @Param("userId") UUID userId);
}
