package com.collabflow.platform.service;

import com.collabflow.platform.dto.request.AddProjectMemberRequest;
import com.collabflow.platform.dto.request.CreateProjectRequest;
import com.collabflow.platform.dto.request.UpdateProjectMemberRoleRequest;
import com.collabflow.platform.dto.request.UpdateProjectRequest;
import com.collabflow.platform.dto.response.ProjectMemberResponse;
import com.collabflow.platform.dto.response.ProjectResponse;
import com.collabflow.platform.dto.response.RoleResponse;
import java.util.List;
import java.util.UUID;

public interface ProjectService {

    ProjectResponse create(UUID actorId, CreateProjectRequest request);

    List<ProjectResponse> listByWorkspace(UUID workspaceId, UUID userId);

    ProjectResponse get(UUID projectId, UUID userId);

    ProjectResponse update(UUID projectId, UUID userId, UpdateProjectRequest request);

    void archive(UUID projectId, UUID userId);

    List<ProjectMemberResponse> listMembers(UUID projectId, UUID userId);

    ProjectMemberResponse addMember(UUID projectId, UUID actorId, AddProjectMemberRequest request);

    ProjectMemberResponse updateMemberRole(
            UUID projectId,
            UUID memberId,
            UUID actorId,
            UpdateProjectMemberRoleRequest request
    );

    void removeMember(UUID projectId, UUID memberId, UUID actorId);

    List<RoleResponse> listProjectRoles();
}
