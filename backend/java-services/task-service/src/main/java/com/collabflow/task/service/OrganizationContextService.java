package com.collabflow.task.service;

import com.collabflow.common.exception.BusinessException;
import com.collabflow.common.exception.ErrorCode;
import com.collabflow.task.repository.OrganizationMemberRefRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;

/** Validates {@code X-Organization-Id} against organization_members (shared DB). */
@Service
public class OrganizationContextService {

    private final OrganizationMemberRefRepository organizationMemberRefRepository;

    public OrganizationContextService(OrganizationMemberRefRepository organizationMemberRefRepository) {
        this.organizationMemberRefRepository = organizationMemberRefRepository;
    }

    public void requireMembership(UUID userId, UUID organizationId) {
        if (organizationId == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "X-Organization-Id header is required");
        }
        if (!organizationMemberRefRepository.existsByOrganizationIdAndUserId(organizationId, userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Not a member of organization " + organizationId);
        }
    }
}
