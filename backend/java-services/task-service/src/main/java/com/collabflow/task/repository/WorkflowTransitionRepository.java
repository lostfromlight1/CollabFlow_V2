package com.collabflow.task.repository;

import com.collabflow.task.model.WorkflowTransition;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkflowTransitionRepository extends JpaRepository<WorkflowTransition, UUID> {

    boolean existsByWorkflowIdAndFromStatusIdAndToStatusId(
            UUID workflowId,
            UUID fromStatusId,
            UUID toStatusId
    );
}
