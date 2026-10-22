package com.collabflow.platform.service;

import com.collabflow.platform.dto.request.AddMemberRequest;
import com.collabflow.platform.dto.request.CreateOrganizationRequest;
import com.collabflow.platform.dto.request.UpdateMemberRoleRequest;
import com.collabflow.platform.dto.request.UpdateOrganizationRequest;
import com.collabflow.platform.dto.response.MemberResponse;
import com.collabflow.platform.dto.response.OrganizationResponse;
import com.collabflow.platform.dto.response.RoleResponse;
import java.util.List;
import java.util.UUID;

public interface OrganizationService {

    OrganizationResponse create(UUID actorId, CreateOrganizationRequest request);

    List<OrganizationResponse> listMine(UUID userId);

    OrganizationResponse get(UUID organizationId, UUID userId);

    OrganizationResponse update(UUID organizationId, UUID userId, UpdateOrganizationRequest request);

    void softDelete(UUID organizationId, UUID userId);

    List<MemberResponse> listMembers(UUID organizationId, UUID userId);

    MemberResponse addMember(UUID organizationId, UUID actorId, AddMemberRequest request);

    MemberResponse updateMemberRole(
            UUID organizationId,
            UUID memberId,
            UUID actorId,
            UpdateMemberRoleRequest request
    );

    void removeMember(UUID organizationId, UUID memberId, UUID actorId);

    List<RoleResponse> listOrganizationRoles();
}
