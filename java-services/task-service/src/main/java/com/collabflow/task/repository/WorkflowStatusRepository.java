package com.collabflow.task.repository;

import com.collabflow.task.model.WorkflowStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkflowStatusRepository extends JpaRepository<WorkflowStatus, UUID> {

    List<WorkflowStatus> findByWorkflowIdOrderByDisplayOrderAsc(UUID workflowId);

    Optional<WorkflowStatus> findByIdAndWorkflowId(UUID id, UUID workflowId);
}
