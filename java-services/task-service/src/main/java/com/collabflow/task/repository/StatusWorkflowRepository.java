package com.collabflow.task.repository;

import com.collabflow.task.model.StatusWorkflow;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StatusWorkflowRepository extends JpaRepository<StatusWorkflow, UUID> {

    Optional<StatusWorkflow> findFirstByDefaultWorkflowTrueAndWorkspaceIdIsNull();
}
