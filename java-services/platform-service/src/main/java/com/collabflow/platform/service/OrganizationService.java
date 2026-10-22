package com.collabflow.platform.service;

import com.collabflow.platform.dto.request.AddMemberRequest;
import com.collabflow.platform.dto.request.AdminCreateOrganizationRequest;
import com.collabflow.platform.dto.request.CreateOrganizationRequest;
import com.collabflow.platform.dto.request.TransferOwnershipRequest;
import com.collabflow.platform.dto.request.UpdateMemberRoleRequest;
import com.collabflow.platform.dto.request.UpdateOrganizationRequest;
import com.collabflow.platform.dto.response.MemberResponse;
import com.collabflow.platform.dto.response.OrganizationResponse;
import com.collabflow.platform.dto.response.RoleResponse;
import java.util.List;
import java.util.UUID;

public interface OrganizationService {

    OrganizationResponse create(UUID actorId, CreateOrganizationRequest request);

    /** Master-admin create: assign a specific owner (not necessarily the admin). */
    OrganizationResponse createAsAdmin(UUID adminId, AdminCreateOrganizationRequest request);

    List<OrganizationResponse> listMine(UUID userId);

    OrganizationResponse get(UUID organizationId, UUID userId);

    OrganizationResponse update(UUID organizationId, UUID userId, UpdateOrganizationRequest request);

    void softDelete(UUID organizationId, UUID userId);

    List<MemberResponse> listMembers(UUID organizationId, UUID userId);

    /**
     * Add member (org manage_members). Usable with X-API-Key + Bearer JWT —
     * Wave 4 invite accept will call this from collab.
     */
    MemberResponse addMember(UUID organizationId, UUID actorId, AddMemberRequest request);

    /** Master-admin attach: no org-role permission required. */
    MemberResponse addMemberAsAdmin(UUID organizationId, UUID adminId, AddMemberRequest request);

    MemberResponse updateMemberRole(
            UUID organizationId,
            UUID memberId,
            UUID actorId,
            UpdateMemberRoleRequest request
    );

    void removeMember(UUID organizationId, UUID memberId, UUID actorId);

    OrganizationResponse transferOwnership(UUID organizationId, UUID actorId, TransferOwnershipRequest request);

    /** Master-admin ownership transfer (bypasses current-owner check). */
    OrganizationResponse transferOwnershipAsAdmin(
            UUID organizationId,
            UUID adminId,
            TransferOwnershipRequest request
    );

    List<RoleResponse> listOrganizationRoles();
}
