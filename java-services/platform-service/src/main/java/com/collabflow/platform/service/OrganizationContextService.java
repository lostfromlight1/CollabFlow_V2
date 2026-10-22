package com.collabflow.platform.service;

import com.collabflow.common.exception.BusinessException;
import com.collabflow.common.exception.ErrorCode;
import com.collabflow.platform.repository.OrganizationMemberRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Validates {@code X-Organization-Id} against real membership — never trust the header alone.
 */
@Service
public class OrganizationContextService {

    private final OrganizationMemberRepository organizationMemberRepository;

    public OrganizationContextService(OrganizationMemberRepository organizationMemberRepository) {
        this.organizationMemberRepository = organizationMemberRepository;
    }

    public UUID requireMembership(UUID userId, UUID organizationId) {
        if (organizationId == null) {
            throw new BusinessException(
                    ErrorCode.BAD_REQUEST,
                    "X-Organization-Id header is required"
            );
        }
        boolean member = organizationMemberRepository
                .findByOrganizationIdAndUserId(organizationId, userId)
                .isPresent();
        if (!member) {
            throw new BusinessException(
                    ErrorCode.FORBIDDEN,
                    "Not a member of organization " + organizationId
            );
        }
        return organizationId;
    }
}
