package com.collabflow.task.controller;

import com.collabflow.common.api.ApiResponse;
import com.collabflow.common.api.PageResponse;
import com.collabflow.common.openapi.OpenApiSupport;
import com.collabflow.common.security.CurrentOrganizationId;
import com.collabflow.common.security.CurrentUserId;
import com.collabflow.common.web.ApiId;
import com.collabflow.task.dto.request.AssignTaskRequest;
import com.collabflow.task.dto.request.ChangeTaskStatusRequest;
import com.collabflow.task.dto.request.CreateSubtaskRequest;
import com.collabflow.task.dto.request.CreateTaskRequest;
import com.collabflow.task.dto.request.UpdateTaskRequest;
import com.collabflow.task.dto.response.TaskResponse;
import com.collabflow.task.dto.response.TaskStatsResponse;
import com.collabflow.task.service.OrganizationContextService;
import com.collabflow.task.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tasks")
@Tag(name = "Tasks")
@SecurityRequirement(name = OpenApiSupport.API_KEY_SCHEME)
@SecurityRequirement(name = OpenApiSupport.BEARER_SCHEME)
public class TaskController {

    private static final Logger log = LoggerFactory.getLogger(TaskController.class);

    private final TaskService taskService;
    private final OrganizationContextService organizationContextService;

    public TaskController(TaskService taskService, OrganizationContextService organizationContextService) {
        this.taskService = taskService;
        this.organizationContextService = organizationContextService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @ApiId("TASK-001")
    @Operation(summary = "Create task")
    public ApiResponse<TaskResponse> create(
            @Valid @RequestBody CreateTaskRequest request,
            @CurrentUserId UUID userId,
            @CurrentOrganizationId UUID organizationId
    ) {
        requireOrg(userId, organizationId);
        log.info("POST /tasks projectId={} userId={}", request.projectId(), userId);
        return ApiResponse.ok("Task created", taskService.create(userId, request));
    }

    @GetMapping
    @ApiId("TASK-002")
    @Operation(summary = "List tasks by project (filters + pagination)")
    public ApiResponse<PageResponse<TaskResponse>> list(
            @RequestParam UUID projectId,
            @RequestParam(required = false) UUID statusId,
            @RequestParam(required = false) UUID assigneeId,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) UUID sprintId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @CurrentUserId UUID userId,
            @CurrentOrganizationId UUID organizationId
    ) {
        requireOrg(userId, organizationId);
        log.info("GET /tasks projectId={} userId={}", projectId, userId);
        return ApiResponse.ok(taskService.listByProject(
                projectId, userId, statusId, assigneeId, priority, sprintId, page, size
        ));
    }

    @GetMapping("/stats")
    @ApiId("TASK-008")
    @Operation(summary = "Basic analytics: task counts by status and priority for a project")
    public ApiResponse<TaskStatsResponse> stats(
            @RequestParam UUID projectId,
            @CurrentUserId UUID userId,
            @CurrentOrganizationId UUID organizationId
    ) {
        requireOrg(userId, organizationId);
        log.info("GET /tasks/stats projectId={} userId={}", projectId, userId);
        return ApiResponse.ok(taskService.statsByProject(projectId, userId));
    }

    @GetMapping("/{id}/subtasks")
    @ApiId("TASK-009")
    @Operation(summary = "List sub-tasks")
    public ApiResponse<List<TaskResponse>> listSubtasks(
            @PathVariable UUID id,
            @CurrentUserId UUID userId,
            @CurrentOrganizationId UUID organizationId
    ) {
        requireOrg(userId, organizationId);
        return ApiResponse.ok(taskService.listSubtasks(id, userId));
    }

    @PostMapping("/{id}/subtasks")
    @ResponseStatus(HttpStatus.CREATED)
    @ApiId("TASK-010")
    @Operation(summary = "Create sub-task")
    public ApiResponse<TaskResponse> createSubtask(
            @PathVariable UUID id,
            @Valid @RequestBody CreateSubtaskRequest request,
            @CurrentUserId UUID userId,
            @CurrentOrganizationId UUID organizationId
    ) {
        requireOrg(userId, organizationId);
        return ApiResponse.ok("Sub-task created", taskService.createSubtask(id, userId, request));
    }

    @GetMapping("/{id}")
    @ApiId("TASK-003")
    @Operation(summary = "Get task")
    public ApiResponse<TaskResponse> get(
            @PathVariable UUID id,
            @CurrentUserId UUID userId,
            @CurrentOrganizationId UUID organizationId
    ) {
        requireOrg(userId, organizationId);
        return ApiResponse.ok(taskService.get(id, userId));
    }

    @PutMapping("/{id}")
    @ApiId("TASK-004")
    @Operation(summary = "Update task (not status/assignee)")
    public ApiResponse<TaskResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateTaskRequest request,
            @CurrentUserId UUID userId,
            @CurrentOrganizationId UUID organizationId
    ) {
        requireOrg(userId, organizationId);
        return ApiResponse.ok("Updated", taskService.update(id, userId, request));
    }

    @DeleteMapping("/{id}")
    @ApiId("TASK-005")
    @Operation(summary = "Delete task")
    public ApiResponse<Void> delete(
            @PathVariable UUID id,
            @CurrentUserId UUID userId,
            @CurrentOrganizationId UUID organizationId
    ) {
        requireOrg(userId, organizationId);
        taskService.delete(id, userId);
        return ApiResponse.okMessage("Task deleted");
    }

    @PutMapping("/{id}/status")
    @ApiId("TASK-006")
    @Operation(summary = "Change task status (transition rules enforced)")
    public ApiResponse<TaskResponse> changeStatus(
            @PathVariable UUID id,
            @Valid @RequestBody ChangeTaskStatusRequest request,
            @CurrentUserId UUID userId,
            @CurrentOrganizationId UUID organizationId
    ) {
        requireOrg(userId, organizationId);
        return ApiResponse.ok("Status updated", taskService.changeStatus(id, userId, request));
    }

    @PutMapping("/{id}/assign")
    @ApiId("TASK-007")
    @Operation(summary = "Assign or unassign task")
    public ApiResponse<TaskResponse> assign(
            @PathVariable UUID id,
            @Valid @RequestBody AssignTaskRequest request,
            @CurrentUserId UUID userId,
            @CurrentOrganizationId UUID organizationId
    ) {
        requireOrg(userId, organizationId);
        return ApiResponse.ok("Assignee updated", taskService.assign(id, userId, request));
    }

    private void requireOrg(UUID userId, UUID organizationId) {
        organizationContextService.requireMembership(userId, organizationId);
    }
}
