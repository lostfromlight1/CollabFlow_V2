package com.collabflow.task.service.impl;

import com.collabflow.common.exception.BusinessException;
import com.collabflow.common.exception.ConflictException;
import com.collabflow.common.exception.ErrorCode;
import com.collabflow.common.exception.ResourceNotFoundException;
import com.collabflow.task.dto.request.AttachTaskToSprintRequest;
import com.collabflow.task.dto.request.CreateSprintRequest;
import com.collabflow.task.dto.request.UpdateSprintRequest;
import com.collabflow.task.dto.response.SprintResponse;
import com.collabflow.task.dto.response.TaskResponse;
import com.collabflow.task.event.TaskUpdatedDomainEvent;
import com.collabflow.task.event.TaskUpdatedPayload;
import com.collabflow.task.model.ProjectRef;
import com.collabflow.task.model.Sprint;
import com.collabflow.task.model.Task;
import com.collabflow.task.model.WorkspaceRef;
import com.collabflow.task.repository.ProjectRefRepository;
import com.collabflow.task.repository.SprintRepository;
import com.collabflow.task.repository.TaskRepository;
import com.collabflow.task.repository.WorkspaceRefRepository;
import com.collabflow.task.security.SystemTaskRoles;
import com.collabflow.task.service.PermissionService;
import com.collabflow.task.service.SprintService;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SprintServiceImpl implements SprintService {

    private static final Logger log = LoggerFactory.getLogger(SprintServiceImpl.class);
    private static final String PROJECT_ARCHIVED = "ARCHIVED";

    private final SprintRepository sprintRepository;
    private final TaskRepository taskRepository;
    private final ProjectRefRepository projectRefRepository;
    private final WorkspaceRefRepository workspaceRefRepository;
    private final PermissionService permissionService;
    private final ApplicationEventPublisher events;

    public SprintServiceImpl(
            SprintRepository sprintRepository,
            TaskRepository taskRepository,
            ProjectRefRepository projectRefRepository,
            WorkspaceRefRepository workspaceRefRepository,
            PermissionService permissionService,
            ApplicationEventPublisher events
    ) {
        this.sprintRepository = sprintRepository;
        this.taskRepository = taskRepository;
        this.projectRefRepository = projectRefRepository;
        this.workspaceRefRepository = workspaceRefRepository;
        this.permissionService = permissionService;
        this.events = events;
    }

    @Override
    @Transactional
    public SprintResponse create(UUID actorId, CreateSprintRequest request) {
        UUID projectId = request.projectId();
        log.info("Creating sprint projectId={} actor={}", projectId, actorId);

        ProjectRef project = requireActiveProject(projectId);
        permissionService.requireProjectPermission(projectId, actorId, SystemTaskRoles.SPRINT_CREATE);

        WorkspaceRef workspace = workspaceRefRepository.findByIdAndActiveTrue(project.getWorkspaceId())
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found"));

        validateDates(request.startDate(), request.endDate());

        Sprint sprint = new Sprint();
        sprint.setName(request.name().trim());
        sprint.setDescription(request.description());
        sprint.setProjectId(projectId);
        sprint.setWorkspaceId(workspace.getId());
        sprint.setOrganizationId(workspace.getOrganizationId());
        sprint.setSprintNumber(sprintRepository.findMaxSprintNumber(projectId) + 1);
        sprint.setStartDate(request.startDate());
        sprint.setEndDate(request.endDate());
        sprint.setStatus(Sprint.STATUS_PLANNING);
        sprint.setGoal(request.goal());
        sprint.setCapacity(request.capacity());
        if (request.backlogOrder() != null) {
            sprint.setBacklogOrder(request.backlogOrder());
        }
        sprintRepository.save(sprint);

        log.info("Created sprint id={} number={} projectId={}", sprint.getId(), sprint.getSprintNumber(), projectId);
        return SprintResponse.from(sprint);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SprintResponse> listByProject(UUID projectId, UUID userId) {
        requireActiveProject(projectId);
        permissionService.requireProjectPermission(projectId, userId, SystemTaskRoles.SPRINT_READ);
        return sprintRepository.findByProjectIdOrderBySprintNumberDesc(projectId).stream()
                .map(SprintResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SprintResponse get(UUID sprintId, UUID userId) {
        Sprint sprint = requireSprint(sprintId);
        permissionService.requireProjectPermission(sprint.getProjectId(), userId, SystemTaskRoles.SPRINT_READ);
        return SprintResponse.from(sprint);
    }

    @Override
    @Transactional
    public SprintResponse update(UUID sprintId, UUID userId, UpdateSprintRequest request) {
        log.info("Updating sprint id={} userId={}", sprintId, userId);
        Sprint sprint = requireSprint(sprintId);
        permissionService.requireProjectPermission(sprint.getProjectId(), userId, SystemTaskRoles.SPRINT_UPDATE);

        if (Sprint.STATUS_COMPLETED.equals(sprint.getStatus())
                || Sprint.STATUS_CANCELLED.equals(sprint.getStatus())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Cannot update a finished sprint");
        }

        if (request.name() != null && !request.name().isBlank()) {
            sprint.setName(request.name().trim());
        }
        if (request.description() != null) {
            sprint.setDescription(request.description());
        }
        if (request.goal() != null) {
            sprint.setGoal(request.goal());
        }
        if (request.capacity() != null) {
            sprint.setCapacity(request.capacity());
        }
        if (request.backlogOrder() != null) {
            sprint.setBacklogOrder(request.backlogOrder());
        }

        InstantPair dates = resolveDates(sprint, request.startDate(), request.endDate());
        sprint.setStartDate(dates.start());
        sprint.setEndDate(dates.end());

        log.info("Updated sprint id={}", sprintId);
        return SprintResponse.from(sprint);
    }

    @Override
    @Transactional
    public void delete(UUID sprintId, UUID userId) {
        log.info("Deleting sprint id={} userId={}", sprintId, userId);
        Sprint sprint = requireSprint(sprintId);
        permissionService.requireProjectPermission(sprint.getProjectId(), userId, SystemTaskRoles.SPRINT_DELETE);

        if (Sprint.STATUS_ACTIVE.equals(sprint.getStatus())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Complete or cancel an active sprint before deleting");
        }

        // ON DELETE SET NULL clears tasks.sprint_id
        sprintRepository.delete(sprint);
        log.info("Deleted sprint id={}", sprintId);
    }

    @Override
    @Transactional
    public SprintResponse start(UUID sprintId, UUID userId) {
        log.info("Starting sprint id={} userId={}", sprintId, userId);
        Sprint sprint = requireSprint(sprintId);
        permissionService.requireProjectPermission(sprint.getProjectId(), userId, SystemTaskRoles.SPRINT_UPDATE);

        if (!Sprint.STATUS_PLANNING.equals(sprint.getStatus())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Only PLANNING sprints can be started");
        }
        if (sprintRepository.existsByProjectIdAndStatus(sprint.getProjectId(), Sprint.STATUS_ACTIVE)) {
            throw new ConflictException("Another sprint is already ACTIVE in this project");
        }

        sprint.setStatus(Sprint.STATUS_ACTIVE);
        log.info("Started sprint id={}", sprintId);
        return SprintResponse.from(sprint);
    }

    @Override
    @Transactional
    public SprintResponse complete(UUID sprintId, UUID userId) {
        log.info("Completing sprint id={} userId={}", sprintId, userId);
        Sprint sprint = requireSprint(sprintId);
        permissionService.requireProjectPermission(sprint.getProjectId(), userId, SystemTaskRoles.SPRINT_UPDATE);

        if (!Sprint.STATUS_ACTIVE.equals(sprint.getStatus())
                && !Sprint.STATUS_PLANNING.equals(sprint.getStatus())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Only PLANNING or ACTIVE sprints can be completed");
        }

        Long velocity = taskRepository.sumStoryPointsBySprintId(sprintId);
        sprint.setVelocity(velocity != null ? velocity.intValue() : 0);
        sprint.setStatus(Sprint.STATUS_COMPLETED);
        log.info("Completed sprint id={} velocity={}", sprintId, sprint.getVelocity());
        return SprintResponse.from(sprint);
    }

    @Override
    @Transactional
    public TaskResponse attachTask(UUID sprintId, UUID userId, AttachTaskToSprintRequest request) {
        Sprint sprint = requireSprint(sprintId);
        permissionService.requireProjectPermission(sprint.getProjectId(), userId, SystemTaskRoles.SPRINT_UPDATE);

        if (Sprint.STATUS_COMPLETED.equals(sprint.getStatus())
                || Sprint.STATUS_CANCELLED.equals(sprint.getStatus())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Cannot attach tasks to a finished sprint");
        }

        Task task = taskRepository.findById(request.taskId())
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));
        if (!task.getProjectId().equals(sprint.getProjectId())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Task must belong to the same project as the sprint");
        }

        task.setSprintId(sprintId);
        events.publishEvent(new TaskUpdatedDomainEvent(new TaskUpdatedPayload(
                task.getId(), task.getProjectId(), task.getName()
        )));
        log.info("Attached task={} to sprint={}", task.getId(), sprintId);
        return TaskResponse.from(task);
    }

    @Override
    @Transactional
    public TaskResponse detachTask(UUID sprintId, UUID taskId, UUID userId) {
        Sprint sprint = requireSprint(sprintId);
        permissionService.requireProjectPermission(sprint.getProjectId(), userId, SystemTaskRoles.SPRINT_UPDATE);

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));
        if (!sprintId.equals(task.getSprintId())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Task is not attached to this sprint");
        }

        task.setSprintId(null);
        events.publishEvent(new TaskUpdatedDomainEvent(new TaskUpdatedPayload(
                task.getId(), task.getProjectId(), task.getName()
        )));
        log.info("Detached task={} from sprint={}", taskId, sprintId);
        return TaskResponse.from(task);
    }

    private ProjectRef requireActiveProject(UUID projectId) {
        return projectRefRepository.findByIdAndStatusNot(projectId, PROJECT_ARCHIVED)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
    }

    private Sprint requireSprint(UUID sprintId) {
        return sprintRepository.findById(sprintId)
                .orElseThrow(() -> new ResourceNotFoundException("Sprint not found"));
    }

    private static void validateDates(java.time.Instant start, java.time.Instant end) {
        if (end.isBefore(start)) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "endDate must be on or after startDate");
        }
    }

    private static InstantPair resolveDates(
            Sprint sprint,
            java.time.Instant startDate,
            java.time.Instant endDate
    ) {
        java.time.Instant start = startDate != null ? startDate : sprint.getStartDate();
        java.time.Instant end = endDate != null ? endDate : sprint.getEndDate();
        validateDates(start, end);
        return new InstantPair(start, end);
    }

    private record InstantPair(java.time.Instant start, java.time.Instant end) {}
}
