package com.collabflow.task.service.impl;

import com.collabflow.common.exception.BusinessException;
import com.collabflow.common.exception.ErrorCode;
import com.collabflow.common.exception.ResourceNotFoundException;
import com.collabflow.task.dto.request.AssignTaskRequest;
import com.collabflow.task.dto.request.ChangeTaskStatusRequest;
import com.collabflow.task.dto.request.CreateTaskRequest;
import com.collabflow.task.dto.request.UpdateTaskRequest;
import com.collabflow.task.dto.response.TaskResponse;
import com.collabflow.task.event.TaskAssignedDomainEvent;
import com.collabflow.task.event.TaskAssignedPayload;
import com.collabflow.task.event.TaskCreatedDomainEvent;
import com.collabflow.task.event.TaskCreatedPayload;
import com.collabflow.task.event.TaskDeletedDomainEvent;
import com.collabflow.task.event.TaskDeletedPayload;
import com.collabflow.task.event.TaskStatusChangedDomainEvent;
import com.collabflow.task.event.TaskStatusChangedPayload;
import com.collabflow.task.event.TaskUpdatedDomainEvent;
import com.collabflow.task.event.TaskUpdatedPayload;
import com.collabflow.task.model.ProjectRef;
import com.collabflow.task.model.Task;
import com.collabflow.task.model.TaskType;
import com.collabflow.task.model.WorkflowStatus;
import com.collabflow.task.model.WorkspaceRef;
import com.collabflow.task.repository.ProjectRefRepository;
import com.collabflow.task.repository.StatusWorkflowRepository;
import com.collabflow.task.repository.TaskRepository;
import com.collabflow.task.repository.TaskTypeRepository;
import com.collabflow.task.repository.WorkflowStatusRepository;
import com.collabflow.task.repository.WorkflowTransitionRepository;
import com.collabflow.task.repository.WorkspaceRefRepository;
import com.collabflow.task.security.SystemTaskRoles;
import com.collabflow.task.service.PermissionService;
import com.collabflow.task.service.TaskService;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskServiceImpl implements TaskService {

    private static final Logger log = LoggerFactory.getLogger(TaskServiceImpl.class);
    private static final String PROJECT_ARCHIVED = "ARCHIVED";
    private static final Set<String> ALLOWED_PRIORITIES = Set.of(
            Task.PRIORITY_LOW, Task.PRIORITY_MEDIUM, Task.PRIORITY_HIGH, Task.PRIORITY_CRITICAL
    );

    private final TaskRepository taskRepository;
    private final TaskTypeRepository taskTypeRepository;
    private final ProjectRefRepository projectRefRepository;
    private final WorkspaceRefRepository workspaceRefRepository;
    private final StatusWorkflowRepository workflowRepository;
    private final WorkflowStatusRepository statusRepository;
    private final WorkflowTransitionRepository transitionRepository;
    private final PermissionService permissionService;
    private final ApplicationEventPublisher events;

    public TaskServiceImpl(
            TaskRepository taskRepository,
            TaskTypeRepository taskTypeRepository,
            ProjectRefRepository projectRefRepository,
            WorkspaceRefRepository workspaceRefRepository,
            StatusWorkflowRepository workflowRepository,
            WorkflowStatusRepository statusRepository,
            WorkflowTransitionRepository transitionRepository,
            PermissionService permissionService,
            ApplicationEventPublisher events
    ) {
        this.taskRepository = taskRepository;
        this.taskTypeRepository = taskTypeRepository;
        this.projectRefRepository = projectRefRepository;
        this.workspaceRefRepository = workspaceRefRepository;
        this.workflowRepository = workflowRepository;
        this.statusRepository = statusRepository;
        this.transitionRepository = transitionRepository;
        this.permissionService = permissionService;
        this.events = events;
    }

    @Override
    @Transactional
    public TaskResponse create(UUID actorId, CreateTaskRequest request) {
        UUID projectId = request.projectId();
        log.info("Creating task projectId={} actor={}", projectId, actorId);

        ProjectRef project = requireActiveProject(projectId);
        permissionService.requireProjectPermission(projectId, actorId, SystemTaskRoles.TASK_CREATE);

        WorkspaceRef workspace = workspaceRefRepository.findByIdAndActiveTrue(project.getWorkspaceId())
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found"));

        UUID workflowId = resolveWorkflowId(project);
        TaskType taskType = resolveTaskType(request.taskTypeId());
        UUID statusId = taskType.getDefaultStatusId() != null
                ? taskType.getDefaultStatusId()
                : SystemTaskRoles.STATUS_TODO;
        WorkflowStatus status = statusRepository.findByIdAndWorkflowId(statusId, workflowId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.VALIDATION_FAILED,
                        "Task type default status is not in the project workflow"
                ));

        if (request.parentTaskId() != null) {
            Task parent = taskRepository.findById(request.parentTaskId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent task not found"));
            if (!parent.getProjectId().equals(projectId)) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Parent task must be in the same project");
            }
        }

        Task task = new Task();
        task.setName(request.name().trim());
        task.setDescription(request.description());
        task.setTaskTypeId(taskType.getId());
        task.setProjectId(projectId);
        task.setWorkspaceId(workspace.getId());
        task.setOrganizationId(workspace.getOrganizationId());
        task.setParentTaskId(request.parentTaskId());
        task.setStatusId(status.getId());
        task.setReporterId(actorId);
        if (request.priority() != null && !request.priority().isBlank()) {
            task.setPriority(normalizePriority(request.priority()));
        }
        task.setStartDate(request.startDate());
        task.setDueDate(request.dueDate());
        task.setEstimatedHours(request.estimatedHours());
        task.setStoryPoints(request.storyPoints());
        taskRepository.save(task);

        events.publishEvent(new TaskCreatedDomainEvent(new TaskCreatedPayload(
                task.getId(), projectId, workspace.getId(), task.getName(), actorId, task.getStatusId()
        )));
        log.info("Created task id={} projectId={}", task.getId(), projectId);
        return TaskResponse.from(task);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskResponse> listByProject(UUID projectId, UUID userId) {
        log.debug("Listing tasks projectId={} userId={}", projectId, userId);
        requireActiveProject(projectId);
        permissionService.requireProjectPermission(projectId, userId, SystemTaskRoles.TASK_READ);
        return taskRepository.findByProjectIdOrderByCreatedAtDesc(projectId).stream()
                .map(TaskResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TaskResponse get(UUID taskId, UUID userId) {
        Task task = requireTask(taskId);
        permissionService.requireProjectPermission(task.getProjectId(), userId, SystemTaskRoles.TASK_READ);
        return TaskResponse.from(task);
    }

    @Override
    @Transactional
    public TaskResponse update(UUID taskId, UUID userId, UpdateTaskRequest request) {
        log.info("Updating task id={} userId={}", taskId, userId);
        Task task = requireTask(taskId);
        permissionService.requireProjectPermission(task.getProjectId(), userId, SystemTaskRoles.TASK_UPDATE);

        if (request.name() != null && !request.name().isBlank()) {
            task.setName(request.name().trim());
        }
        if (request.description() != null) {
            task.setDescription(request.description());
        }
        if (request.priority() != null && !request.priority().isBlank()) {
            task.setPriority(normalizePriority(request.priority()));
        }
        if (request.startDate() != null) {
            task.setStartDate(request.startDate());
        }
        if (request.dueDate() != null) {
            task.setDueDate(request.dueDate());
        }
        if (request.estimatedHours() != null) {
            task.setEstimatedHours(request.estimatedHours());
        }
        if (request.actualHours() != null) {
            task.setActualHours(request.actualHours());
        }
        if (request.storyPoints() != null) {
            task.setStoryPoints(request.storyPoints());
        }

        events.publishEvent(new TaskUpdatedDomainEvent(new TaskUpdatedPayload(
                task.getId(), task.getProjectId(), task.getName()
        )));
        log.info("Updated task id={}", taskId);
        return TaskResponse.from(task);
    }

    @Override
    @Transactional
    public void delete(UUID taskId, UUID userId) {
        log.info("Deleting task id={} userId={}", taskId, userId);
        Task task = requireTask(taskId);
        permissionService.requireProjectPermission(task.getProjectId(), userId, SystemTaskRoles.TASK_DELETE);
        UUID projectId = task.getProjectId();
        taskRepository.delete(task);
        events.publishEvent(new TaskDeletedDomainEvent(new TaskDeletedPayload(taskId, projectId)));
        log.info("Deleted task id={}", taskId);
    }

    @Override
    @Transactional
    public TaskResponse changeStatus(UUID taskId, UUID userId, ChangeTaskStatusRequest request) {
        log.info("Changing status task={} userId={} to={}", taskId, userId, request.statusId());
        Task task = requireTask(taskId);
        permissionService.requireProjectPermission(task.getProjectId(), userId, SystemTaskRoles.TASK_MOVE);

        ProjectRef project = requireActiveProject(task.getProjectId());
        UUID workflowId = resolveWorkflowId(project);
        UUID fromStatusId = task.getStatusId();
        UUID toStatusId = request.statusId();

        if (fromStatusId.equals(toStatusId)) {
            return TaskResponse.from(task);
        }

        WorkflowStatus toStatus = statusRepository.findByIdAndWorkflowId(toStatusId, workflowId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.VALIDATION_FAILED,
                        "Target status is not in the project workflow"
                ));

        if (!transitionRepository.existsByWorkflowIdAndFromStatusIdAndToStatusId(
                workflowId, fromStatusId, toStatusId
        )) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Status transition is not allowed");
        }

        task.setStatusId(toStatus.getId());
        if (WorkflowStatus.CATEGORY_DONE.equals(toStatus.getCategory())) {
            task.setClosedAt(Instant.now());
        } else {
            task.setClosedAt(null);
        }

        events.publishEvent(new TaskStatusChangedDomainEvent(new TaskStatusChangedPayload(
                task.getId(), task.getProjectId(), fromStatusId, toStatusId
        )));
        log.info("Changed status task={} {} -> {}", taskId, fromStatusId, toStatusId);
        return TaskResponse.from(task);
    }

    @Override
    @Transactional
    public TaskResponse assign(UUID taskId, UUID userId, AssignTaskRequest request) {
        log.info("Assigning task={} userId={} assignee={}", taskId, userId, request.assigneeId());
        Task task = requireTask(taskId);
        permissionService.requireProjectPermission(task.getProjectId(), userId, SystemTaskRoles.TASK_ASSIGN);

        UUID previous = task.getAssigneeId();
        UUID assigneeId = request.assigneeId();
        if (assigneeId != null) {
            if (!permissionService.isProjectMember(task.getProjectId(), assigneeId)) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Assignee must be a project member");
            }
        }
        task.setAssigneeId(assigneeId);

        events.publishEvent(new TaskAssignedDomainEvent(new TaskAssignedPayload(
                task.getId(), task.getProjectId(), assigneeId, previous
        )));
        log.info("Assigned task={} assignee={}", taskId, assigneeId);
        return TaskResponse.from(task);
    }

    private ProjectRef requireActiveProject(UUID projectId) {
        return projectRefRepository.findByIdAndStatusNot(projectId, PROJECT_ARCHIVED)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
    }

    private Task requireTask(UUID taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));
    }

    private UUID resolveWorkflowId(ProjectRef project) {
        if (project.getWorkflowId() != null) {
            return project.getWorkflowId();
        }
        return workflowRepository.findFirstByDefaultWorkflowTrueAndWorkspaceIdIsNull()
                .orElseThrow(() -> new IllegalStateException("Default workflow not seeded"))
                .getId();
    }

    private TaskType resolveTaskType(UUID taskTypeId) {
        UUID id = taskTypeId != null ? taskTypeId : SystemTaskRoles.TASK_TYPE_TASK;
        return taskTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task type not found"));
    }

    private static String normalizePriority(String priority) {
        String value = priority.trim().toUpperCase(Locale.ROOT);
        if (!ALLOWED_PRIORITIES.contains(value)) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Invalid priority: " + priority);
        }
        return value;
    }
}
