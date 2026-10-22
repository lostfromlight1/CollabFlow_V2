package com.collabflow.task.repository;

import com.collabflow.task.model.WorkspaceRef;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkspaceRefRepository extends JpaRepository<WorkspaceRef, UUID> {

    Optional<WorkspaceRef> findByIdAndActiveTrue(UUID id);
}
