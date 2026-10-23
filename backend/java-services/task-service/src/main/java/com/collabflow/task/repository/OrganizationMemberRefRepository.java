package com.collabflow.task.repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.collabflow.task.model.OrganizationMemberRef;

public interface OrganizationMemberRefRepository extends JpaRepository<OrganizationMemberRef, UUID> {

    @Query(value = """
            SELECT COUNT(*) > 0
            FROM organization_members
            WHERE organization_id = :organizationId
              AND user_id = :userId
            """, nativeQuery = true)
    boolean existsByOrganizationIdAndUserId(
            @Param("organizationId") UUID organizationId,
            @Param("userId") UUID userId
    );
}
