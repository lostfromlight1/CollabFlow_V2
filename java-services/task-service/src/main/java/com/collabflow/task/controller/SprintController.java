package com.collabflow.task.controller;

import com.collabflow.common.api.ApiResponse;
import com.collabflow.common.openapi.OpenApiSupport;
import com.collabflow.common.security.CurrentOrganizationId;
import com.collabflow.common.security.CurrentUserId;
import com.collabflow.common.web.ApiId;
import com.collabflow.task.dto.request.AttachTaskToSprintRequest;
import com.collabflow.task.dto.request.CreateSprintRequest;
import com.collabflow.task.dto.request.UpdateSprintRequest;
import com.collabflow.task.dto.response.SprintResponse;
import com.collabflow.task.dto.response.TaskResponse;
import com.collabflow.task.service.OrganizationContextService;
import com.collabflow.task.service.SprintService;
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
@RequestMapping("/api/v1/sprints")
@Tag(name = "Sprints")
@SecurityRequirement(name = OpenApiSupport.API_KEY_SCHEME)
@SecurityRequirement(name = OpenApiSupport.BEARER_SCHEME)
public class SprintController {

    private static final Logger log = LoggerFactory.getLogger(SprintController.class);

    private final SprintService sprintService;
    private final OrganizationContextService organizationContextService;

    public SprintController(SprintService sprintService, OrganizationContextService organizationContextService) {
        this.sprintService = sprintService;
        this.organizationContextService = organizationContextService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @ApiId("SPRINT-001")
    @Operation(summary = "Create sprint for a project")
    public ApiResponse<SprintResponse> create(
            @Valid @RequestBody CreateSprintRequest request,
            @CurrentUserId UUID userId,
            @CurrentOrganizationId UUID organizationId
    ) {
        requireOrg(userId, organizationId);
        log.info("POST /sprints projectId={} userId={}", request.projectId(), userId);
        return ApiResponse.ok("Sprint created", sprintService.create(userId, request));
    }

    @GetMapping
    @ApiId("SPRINT-002")
    @Operation(summary = "List sprints by project")
    public ApiResponse<List<SprintResponse>> list(
            @RequestParam UUID projectId,
            @CurrentUserId UUID userId,
            @CurrentOrganizationId UUID organizationId
    ) {
        requireOrg(userId, organizationId);
        log.info("GET /sprints projectId={} userId={}", projectId, userId);
        return ApiResponse.ok(sprintService.listByProject(projectId, userId));
    }

    @GetMapping("/{id}")
    @ApiId("SPRINT-003")
    @Operation(summary = "Get sprint")
    public ApiResponse<SprintResponse> get(
            @PathVariable UUID id,
            @CurrentUserId UUID userId,
            @CurrentOrganizationId UUID organizationId
    ) {
        requireOrg(userId, organizationId);
        log.info("GET /sprints/{} userId={}", id, userId);
        return ApiResponse.ok(sprintService.get(id, userId));
    }

    @PutMapping("/{id}")
    @ApiId("SPRINT-004")
    @Operation(summary = "Update sprint (not status — use start/complete)")
    public ApiResponse<SprintResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateSprintRequest request,
            @CurrentUserId UUID userId,
            @CurrentOrganizationId UUID organizationId
    ) {
        requireOrg(userId, organizationId);
        log.info("PUT /sprints/{} userId={}", id, userId);
        return ApiResponse.ok("Updated", sprintService.update(id, userId, request));
    }

    @DeleteMapping("/{id}")
    @ApiId("SPRINT-005")
    @Operation(summary = "Delete sprint (tasks.sprint_id cleared)")
    public ApiResponse<Void> delete(
            @PathVariable UUID id,
            @CurrentUserId UUID userId,
            @CurrentOrganizationId UUID organizationId
    ) {
        requireOrg(userId, organizationId);
        log.info("DELETE /sprints/{} userId={}", id, userId);
        sprintService.delete(id, userId);
        return ApiResponse.okMessage("Sprint deleted");
    }

    @PostMapping("/{id}/start")
    @ApiId("SPRINT-006")
    @Operation(summary = "Start sprint (PLANNING → ACTIVE; one ACTIVE per project)")
    public ApiResponse<SprintResponse> start(
            @PathVariable UUID id,
            @CurrentUserId UUID userId,
            @CurrentOrganizationId UUID organizationId
    ) {
        requireOrg(userId, organizationId);
        log.info("POST /sprints/{}/start userId={}", id, userId);
        return ApiResponse.ok("Sprint started", sprintService.start(id, userId));
    }

    @PostMapping("/{id}/complete")
    @ApiId("SPRINT-007")
    @Operation(summary = "Complete sprint; sets velocity from story points")
    public ApiResponse<SprintResponse> complete(
            @PathVariable UUID id,
            @CurrentUserId UUID userId,
            @CurrentOrganizationId UUID organizationId
    ) {
        requireOrg(userId, organizationId);
        log.info("POST /sprints/{}/complete userId={}", id, userId);
        return ApiResponse.ok("Sprint completed", sprintService.complete(id, userId));
    }

    @PostMapping("/{id}/tasks")
    @ApiId("SPRINT-008")
    @Operation(summary = "Attach a task to this sprint")
    public ApiResponse<TaskResponse> attachTask(
            @PathVariable UUID id,
            @Valid @RequestBody AttachTaskToSprintRequest request,
            @CurrentUserId UUID userId,
            @CurrentOrganizationId UUID organizationId
    ) {
        requireOrg(userId, organizationId);
        log.info("POST /sprints/{}/tasks taskId={} userId={}", id, request.taskId(), userId);
        return ApiResponse.ok("Task attached", sprintService.attachTask(id, userId, request));
    }

    @DeleteMapping("/{id}/tasks/{taskId}")
    @ApiId("SPRINT-009")
    @Operation(summary = "Detach a task from this sprint")
    public ApiResponse<TaskResponse> detachTask(
            @PathVariable UUID id,
            @PathVariable UUID taskId,
            @CurrentUserId UUID userId,
            @CurrentOrganizationId UUID organizationId
    ) {
        requireOrg(userId, organizationId);
        log.info("DELETE /sprints/{}/tasks/{} userId={}", id, taskId, userId);
        return ApiResponse.ok("Task detached", sprintService.detachTask(id, taskId, userId));
    }

    private void requireOrg(UUID userId, UUID organizationId) {
        organizationContextService.requireMembership(userId, organizationId);
    }
}
