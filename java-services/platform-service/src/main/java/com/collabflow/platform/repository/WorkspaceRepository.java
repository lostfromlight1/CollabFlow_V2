package com.collabflow.platform.repository;

import com.collabflow.platform.model.Workspace;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkspaceRepository extends JpaRepository<Workspace, UUID> {

    Optional<Workspace> findByIdAndActiveTrue(UUID id);

    List<Workspace> findByOrganizationIdAndActiveTrueOrderByNameAsc(UUID organizationId);

    boolean existsByInviteCodeIgnoreCase(String inviteCode);
}
