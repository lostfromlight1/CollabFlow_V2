package com.collabflow.platform.repository;

import com.collabflow.platform.model.Project;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectRepository extends JpaRepository<Project, UUID> {

    Optional<Project> findByIdAndStatusNot(UUID id, String status);

    List<Project> findByWorkspaceIdAndStatusOrderByNameAsc(UUID workspaceId, String status);
}
