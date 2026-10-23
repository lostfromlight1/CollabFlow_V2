package com.collabflow.auth.repository;

import com.collabflow.auth.model.OrganizationMemberRef;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationMemberRefRepository extends JpaRepository<OrganizationMemberRef, UUID> {

    List<OrganizationMemberRef> findByUserId(UUID userId);

    Optional<OrganizationMemberRef> findByUserIdAndOrganizationId(UUID userId, UUID organizationId);
}
