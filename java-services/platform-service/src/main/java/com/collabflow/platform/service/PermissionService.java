package com.collabflow.platform.service;

import com.collabflow.common.exception.BusinessException;
import com.collabflow.common.exception.ErrorCode;
import com.collabflow.platform.repository.OrganizationMemberRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class PermissionService {

    private final OrganizationMemberRepository memberRepository;

    public PermissionService(OrganizationMemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    public void requireOrgPermission(UUID organizationId, UUID userId, String permission) {
        if (memberRepository.countWithPermission(organizationId, userId, permission) == 0) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Missing permission: " + permission);
        }
    }

    public void requireOrgMember(UUID organizationId, UUID userId) {
        if (!memberRepository.existsByOrganizationIdAndUserId(organizationId, userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Not a member of this organization");
        }
    }
}
