package com.collabflow.platform.controller;

import com.collabflow.common.api.ApiResponse;
import com.collabflow.common.openapi.OpenApiSupport;
import com.collabflow.common.security.CurrentUserId;
import com.collabflow.common.web.ApiId;
import com.collabflow.platform.dto.request.AddWorkspaceMemberRequest;
import com.collabflow.platform.dto.request.CreateWorkspaceRequest;
import com.collabflow.platform.dto.request.JoinWorkspaceRequest;
import com.collabflow.platform.dto.request.UpdateWorkspaceMemberRoleRequest;
import com.collabflow.platform.dto.request.UpdateWorkspaceRequest;
import com.collabflow.platform.dto.response.WorkspaceMemberResponse;
import com.collabflow.platform.dto.response.WorkspaceResponse;
import com.collabflow.platform.service.WorkspaceService;
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
@RequestMapping("/api/v1/workspaces")
@Tag(name = "Workspaces")
@SecurityRequirement(name = OpenApiSupport.API_KEY_SCHEME)
@SecurityRequirement(name = OpenApiSupport.BEARER_SCHEME)
public class WorkspaceController {

    private static final Logger log = LoggerFactory.getLogger(WorkspaceController.class);

    private final WorkspaceService workspaceService;

    public WorkspaceController(WorkspaceService workspaceService) {
        this.workspaceService = workspaceService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @ApiId("WS-001")
    @Operation(summary = "Create workspace (caller becomes Workspace Admin)")
    public ApiResponse<WorkspaceResponse> create(
            @Valid @RequestBody CreateWorkspaceRequest request,
            @CurrentUserId UUID userId
    ) {
        log.info("POST /workspaces orgId={} userId={}", request.organizationId(), userId);
        return ApiResponse.ok("Workspace created", workspaceService.create(userId, request));
    }

    @GetMapping("/{id}")
    @ApiId("WS-003")
    @Operation(summary = "Get workspace")
    public ApiResponse<WorkspaceResponse> get(@PathVariable UUID id, @CurrentUserId UUID userId) {
        log.info("GET /workspaces/{} userId={}", id, userId);
        return ApiResponse.ok(workspaceService.get(id, userId));
    }

    @PutMapping("/{id}")
    @ApiId("WS-004")
    @Operation(summary = "Update workspace")
    public ApiResponse<WorkspaceResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateWorkspaceRequest request,
            @CurrentUserId UUID userId
    ) {
        log.info("PUT /workspaces/{} userId={}", id, userId);
        return ApiResponse.ok("Updated", workspaceService.update(id, userId, request));
    }

    @DeleteMapping("/{id}")
    @ApiId("WS-005")
    @Operation(summary = "Soft-delete workspace")
    public ApiResponse<Void> delete(@PathVariable UUID id, @CurrentUserId UUID userId) {
        log.info("DELETE /workspaces/{} userId={}", id, userId);
        workspaceService.softDelete(id, userId);
        return ApiResponse.okMessage("Workspace deactivated");
    }

    @GetMapping("/{id}/members")
    @ApiId("WS-006")
    @Operation(summary = "List workspace members")
    public ApiResponse<List<WorkspaceMemberResponse>> listMembers(
            @PathVariable UUID id,
            @CurrentUserId UUID userId
    ) {
        log.info("GET /workspaces/{}/members userId={}", id, userId);
        return ApiResponse.ok(workspaceService.listMembers(id, userId));
    }

    @PostMapping("/{id}/members")
    @ResponseStatus(HttpStatus.CREATED)
    @ApiId("WS-007")
    @Operation(summary = "Add workspace member")
    public ApiResponse<WorkspaceMemberResponse> addMember(
            @PathVariable UUID id,
            @Valid @RequestBody AddWorkspaceMemberRequest request,
            @CurrentUserId UUID userId
    ) {
        log.info("POST /workspaces/{}/members userId={} newUser={}", id, userId, request.userId());
        return ApiResponse.ok("Member added", workspaceService.addMember(id, userId, request));
    }

    @PutMapping("/{id}/members/{memberId}")
    @ApiId("WS-008")
    @Operation(summary = "Update workspace member role")
    public ApiResponse<WorkspaceMemberResponse> updateMember(
            @PathVariable UUID id,
            @PathVariable UUID memberId,
            @Valid @RequestBody UpdateWorkspaceMemberRoleRequest request,
            @CurrentUserId UUID userId
    ) {
        log.info("PUT /workspaces/{}/members/{} userId={}", id, memberId, userId);
        return ApiResponse.ok("Role updated", workspaceService.updateMemberRole(id, memberId, userId, request));
    }

    @DeleteMapping("/{id}/members/{memberId}")
    @ApiId("WS-009")
    @Operation(summary = "Remove workspace member")
    public ApiResponse<Void> removeMember(
            @PathVariable UUID id,
            @PathVariable UUID memberId,
            @CurrentUserId UUID userId
    ) {
        log.info("DELETE /workspaces/{}/members/{} userId={}", id, memberId, userId);
        workspaceService.removeMember(id, memberId, userId);
        return ApiResponse.okMessage("Member removed");
    }

    @PostMapping("/{id}/join")
    @ResponseStatus(HttpStatus.CREATED)
    @ApiId("WS-010")
    @Operation(summary = "Join workspace via invite code (must be org member)")
    public ApiResponse<WorkspaceMemberResponse> join(
            @PathVariable UUID id,
            @Valid @RequestBody JoinWorkspaceRequest request,
            @CurrentUserId UUID userId
    ) {
        log.info("POST /workspaces/{}/join userId={}", id, userId);
        return ApiResponse.ok("Joined workspace", workspaceService.join(id, userId, request));
    }
}
