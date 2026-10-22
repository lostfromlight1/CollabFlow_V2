package com.collabflow.platform.service;

import com.collabflow.common.exception.BusinessException;
import com.collabflow.common.exception.ErrorCode;
import com.collabflow.platform.repository.OrganizationMemberRepository;
import com.collabflow.platform.repository.ProjectMemberRepository;
import com.collabflow.platform.repository.WorkspaceMemberRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class PermissionService {

    private final OrganizationMemberRepository orgMemberRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final ProjectMemberRepository projectMemberRepository;

    public PermissionService(
            OrganizationMemberRepository orgMemberRepository,
            WorkspaceMemberRepository workspaceMemberRepository,
            ProjectMemberRepository projectMemberRepository
    ) {
        this.orgMemberRepository = orgMemberRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.projectMemberRepository = projectMemberRepository;
    }

    public void requireOrgPermission(UUID organizationId, UUID userId, String permission) {
        if (orgMemberRepository.countWithPermission(organizationId, userId, permission) == 0) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Missing permission: " + permission);
        }
    }

    public void requireOrgMember(UUID organizationId, UUID userId) {
        if (!orgMemberRepository.existsByOrganizationIdAndUserId(organizationId, userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Not a member of this organization");
        }
    }

    public void requireWorkspacePermission(UUID workspaceId, UUID userId, String permission) {
        if (workspaceMemberRepository.countWithPermission(workspaceId, userId, permission) == 0) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Missing permission: " + permission);
        }
    }

    public void requireWorkspaceMember(UUID workspaceId, UUID userId) {
        if (!workspaceMemberRepository.existsByWorkspaceIdAndUserId(workspaceId, userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Not a member of this workspace");
        }
    }

    public void requireProjectPermission(UUID projectId, UUID userId, String permission) {
        if (projectMemberRepository.countWithPermission(projectId, userId, permission) == 0) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Missing permission: " + permission);
        }
    }

    public void requireProjectMember(UUID projectId, UUID userId) {
        if (!projectMemberRepository.existsByProjectIdAndUserId(projectId, userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Not a member of this project");
        }
    }
}
