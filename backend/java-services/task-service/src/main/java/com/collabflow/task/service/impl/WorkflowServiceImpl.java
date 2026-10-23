package com.collabflow.task.service.impl;

import com.collabflow.common.exception.ResourceNotFoundException;
import com.collabflow.task.dto.response.TaskTypeResponse;
import com.collabflow.task.dto.response.WorkflowResponse;
import com.collabflow.task.dto.response.WorkflowStatusResponse;
import com.collabflow.task.model.StatusWorkflow;
import com.collabflow.task.repository.StatusWorkflowRepository;
import com.collabflow.task.repository.TaskTypeRepository;
import com.collabflow.task.repository.WorkflowStatusRepository;
import com.collabflow.task.service.WorkflowService;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkflowServiceImpl implements WorkflowService {

    private static final Logger log = LoggerFactory.getLogger(WorkflowServiceImpl.class);

    private final StatusWorkflowRepository workflowRepository;
    private final WorkflowStatusRepository statusRepository;
    private final TaskTypeRepository taskTypeRepository;

    public WorkflowServiceImpl(
            StatusWorkflowRepository workflowRepository,
            WorkflowStatusRepository statusRepository,
            TaskTypeRepository taskTypeRepository
    ) {
        this.workflowRepository = workflowRepository;
        this.statusRepository = statusRepository;
        this.taskTypeRepository = taskTypeRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public WorkflowResponse getDefault() {
        log.debug("Get default workflow");
        StatusWorkflow workflow = workflowRepository.findFirstByDefaultWorkflowTrueAndWorkspaceIdIsNull()
                .orElseThrow(() -> new ResourceNotFoundException("Default workflow not found"));
        return toResponse(workflow);
    }

    @Override
    @Transactional(readOnly = true)
    public WorkflowResponse get(UUID workflowId) {
        log.debug("Get workflow id={}", workflowId);
        StatusWorkflow workflow = workflowRepository.findById(workflowId)
                .orElseThrow(() -> new ResourceNotFoundException("Workflow not found"));
        return toResponse(workflow);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkflowStatusResponse> listStatuses(UUID workflowId) {
        log.debug("List statuses workflowId={}", workflowId);
        if (!workflowRepository.existsById(workflowId)) {
            throw new ResourceNotFoundException("Workflow not found");
        }
        return statusRepository.findByWorkflowIdOrderByDisplayOrderAsc(workflowId).stream()
                .map(WorkflowStatusResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskTypeResponse> listTaskTypes() {
        return taskTypeRepository.findBySystemTrueOrderByNameAsc().stream()
                .map(TaskTypeResponse::from)
                .toList();
    }

    private WorkflowResponse toResponse(StatusWorkflow workflow) {
        List<WorkflowStatusResponse> statuses = statusRepository
                .findByWorkflowIdOrderByDisplayOrderAsc(workflow.getId()).stream()
                .map(WorkflowStatusResponse::from)
                .toList();
        return WorkflowResponse.from(workflow, statuses);
    }
}
