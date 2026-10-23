package com.collabflow.platform.controller;

import com.collabflow.common.api.ApiResponse;
import com.collabflow.common.openapi.OpenApiSupport;
import com.collabflow.common.security.CurrentUserId;
import com.collabflow.common.web.ApiId;
import com.collabflow.platform.dto.request.AddProjectMemberRequest;
import com.collabflow.platform.dto.request.CreateProjectRequest;
import com.collabflow.platform.dto.request.UpdateProjectMemberRoleRequest;
import com.collabflow.platform.dto.request.UpdateProjectRequest;
import com.collabflow.platform.dto.response.ProjectMemberResponse;
import com.collabflow.platform.dto.response.ProjectResponse;
import com.collabflow.platform.service.ProjectService;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/projects")
@Tag(name = "Projects")
@SecurityRequirement(name = OpenApiSupport.API_KEY_SCHEME)
@SecurityRequirement(name = OpenApiSupport.BEARER_SCHEME)
public class ProjectController {

    private static final Logger log = LoggerFactory.getLogger(ProjectController.class);

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @ApiId("PRJ-001")
    @Operation(summary = "Create project (caller becomes Project Admin)")
    public ApiResponse<ProjectResponse> create(
            @Valid @RequestBody CreateProjectRequest request,
            @CurrentUserId UUID userId
    ) {
        log.info("POST /projects workspaceId={} userId={}", request.workspaceId(), userId);
        return ApiResponse.ok("Project created", projectService.create(userId, request));
    }

    @GetMapping("/{id}")
    @ApiId("PRJ-003")
    @Operation(summary = "Get project")
    public ApiResponse<ProjectResponse> get(@PathVariable UUID id, @CurrentUserId UUID userId) {
        log.info("GET /projects/{} userId={}", id, userId);
        return ApiResponse.ok(projectService.get(id, userId));
    }

    @PutMapping("/{id}")
    @ApiId("PRJ-004")
    @Operation(summary = "Update project")
    public ApiResponse<ProjectResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateProjectRequest request,
            @CurrentUserId UUID userId
    ) {
        log.info("PUT /projects/{} userId={}", id, userId);
        return ApiResponse.ok("Updated", projectService.update(id, userId, request));
    }

    @DeleteMapping("/{id}")
    @ApiId("PRJ-005")
    @Operation(summary = "Archive project (status = ARCHIVED)")
    public ApiResponse<Void> delete(@PathVariable UUID id, @CurrentUserId UUID userId) {
        log.info("DELETE /projects/{} userId={}", id, userId);
        projectService.archive(id, userId);
        return ApiResponse.okMessage("Project archived");
    }

    @GetMapping("/{id}/members")
    @ApiId("PRJ-006")
    @Operation(summary = "List project members")
    public ApiResponse<List<ProjectMemberResponse>> listMembers(
            @PathVariable UUID id,
            @CurrentUserId UUID userId
    ) {
        log.info("GET /projects/{}/members userId={}", id, userId);
        return ApiResponse.ok(projectService.listMembers(id, userId));
    }

    @PostMapping("/{id}/members")
    @ResponseStatus(HttpStatus.CREATED)
    @ApiId("PRJ-007")
    @Operation(summary = "Add project member (must already be workspace member)")
    public ApiResponse<ProjectMemberResponse> addMember(
            @PathVariable UUID id,
            @Valid @RequestBody AddProjectMemberRequest request,
            @CurrentUserId UUID userId
    ) {
        log.info("POST /projects/{}/members userId={} newUser={}", id, userId, request.userId());
        return ApiResponse.ok("Member added", projectService.addMember(id, userId, request));
    }

    @PutMapping("/{id}/members/{memberId}")
    @ApiId("PRJ-008")
    @Operation(summary = "Update project member role")
    public ApiResponse<ProjectMemberResponse> updateMember(
            @PathVariable UUID id,
            @PathVariable UUID memberId,
            @Valid @RequestBody UpdateProjectMemberRoleRequest request,
            @CurrentUserId UUID userId
    ) {
        log.info("PUT /projects/{}/members/{} userId={}", id, memberId, userId);
        return ApiResponse.ok("Role updated", projectService.updateMemberRole(id, memberId, userId, request));
    }

    @DeleteMapping("/{id}/members/{memberId}")
    @ApiId("PRJ-009")
    @Operation(summary = "Remove project member")
    public ApiResponse<Void> removeMember(
            @PathVariable UUID id,
            @PathVariable UUID memberId,
            @CurrentUserId UUID userId
    ) {
        log.info("DELETE /projects/{}/members/{} userId={}", id, memberId, userId);
        projectService.removeMember(id, memberId, userId);
        return ApiResponse.okMessage("Member removed");
    }
}
