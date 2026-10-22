package com.collabflow.platform.service;

import com.collabflow.common.exception.BusinessException;
import com.collabflow.common.exception.ErrorCode;
import com.collabflow.platform.repository.OrganizationMemberRepository;
import com.collabflow.platform.repository.WorkspaceMemberRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class PermissionService {

    private final OrganizationMemberRepository orgMemberRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;

    public PermissionService(
            OrganizationMemberRepository orgMemberRepository,
            WorkspaceMemberRepository workspaceMemberRepository
    ) {
        this.orgMemberRepository = orgMemberRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
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
}
