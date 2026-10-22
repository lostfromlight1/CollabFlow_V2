package com.collabflow.platform.service;

import com.collabflow.platform.dto.request.AddWorkspaceMemberRequest;
import com.collabflow.platform.dto.request.CreateWorkspaceRequest;
import com.collabflow.platform.dto.request.JoinWorkspaceRequest;
import com.collabflow.platform.dto.request.UpdateWorkspaceMemberRoleRequest;
import com.collabflow.platform.dto.request.UpdateWorkspaceRequest;
import com.collabflow.platform.dto.response.RoleResponse;
import com.collabflow.platform.dto.response.WorkspaceMemberResponse;
import com.collabflow.platform.dto.response.WorkspaceResponse;
import java.util.List;
import java.util.UUID;

public interface WorkspaceService {

    WorkspaceResponse create(UUID actorId, CreateWorkspaceRequest request);

    List<WorkspaceResponse> listByOrganization(UUID organizationId, UUID userId);

    WorkspaceResponse get(UUID workspaceId, UUID userId);

    WorkspaceResponse update(UUID workspaceId, UUID userId, UpdateWorkspaceRequest request);

    void softDelete(UUID workspaceId, UUID userId);

    List<WorkspaceMemberResponse> listMembers(UUID workspaceId, UUID userId);

    WorkspaceMemberResponse addMember(UUID workspaceId, UUID actorId, AddWorkspaceMemberRequest request);

    WorkspaceMemberResponse updateMemberRole(
            UUID workspaceId,
            UUID memberId,
            UUID actorId,
            UpdateWorkspaceMemberRoleRequest request
    );

    void removeMember(UUID workspaceId, UUID memberId, UUID actorId);

    WorkspaceMemberResponse join(UUID workspaceId, UUID userId, JoinWorkspaceRequest request);

    List<RoleResponse> listWorkspaceRoles();
}
