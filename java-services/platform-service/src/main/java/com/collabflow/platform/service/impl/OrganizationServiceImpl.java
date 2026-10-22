package com.collabflow.platform.service.impl;

import com.collabflow.common.exception.BusinessException;
import com.collabflow.common.exception.ConflictException;
import com.collabflow.common.exception.ErrorCode;
import com.collabflow.common.exception.ForbiddenException;
import com.collabflow.common.exception.ResourceNotFoundException;
import com.collabflow.platform.config.PlatformProperties;
import com.collabflow.platform.dto.request.AddMemberRequest;
import com.collabflow.platform.dto.request.AdminCreateOrganizationRequest;
import com.collabflow.platform.dto.request.CreateOrganizationRequest;
import com.collabflow.platform.dto.request.TransferOwnershipRequest;
import com.collabflow.platform.dto.request.UpdateMemberRoleRequest;
import com.collabflow.platform.dto.request.UpdateOrganizationRequest;
import com.collabflow.platform.dto.response.MemberResponse;
import com.collabflow.platform.dto.response.OrganizationResponse;
import com.collabflow.platform.dto.response.RoleResponse;
import com.collabflow.platform.event.MemberAddedDomainEvent;
import com.collabflow.platform.event.MemberAddedPayload;
import com.collabflow.platform.event.MemberRemovedDomainEvent;
import com.collabflow.platform.event.MemberRemovedPayload;
import com.collabflow.platform.event.OrganizationCreatedDomainEvent;
import com.collabflow.platform.event.OrganizationCreatedPayload;
import com.collabflow.platform.event.OrganizationDeletedDomainEvent;
import com.collabflow.platform.event.OrganizationDeletedPayload;
import com.collabflow.platform.event.OrganizationUpdatedDomainEvent;
import com.collabflow.platform.event.OrganizationUpdatedPayload;
import com.collabflow.platform.model.Organization;
import com.collabflow.platform.model.OrganizationMember;
import com.collabflow.platform.model.Role;
import com.collabflow.platform.repository.OrganizationMemberRepository;
import com.collabflow.platform.repository.OrganizationRepository;
import com.collabflow.platform.repository.RoleRepository;
import com.collabflow.platform.repository.UserRefRepository;
import com.collabflow.platform.security.SystemRoles;
import com.collabflow.platform.service.OrganizationService;
import com.collabflow.platform.service.PermissionService;
import com.collabflow.platform.service.WorkspaceService;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrganizationServiceImpl implements OrganizationService {

    private static final Logger log = LoggerFactory.getLogger(OrganizationServiceImpl.class);

    private final OrganizationRepository organizationRepository;
    private final OrganizationMemberRepository memberRepository;
    private final RoleRepository roleRepository;
    private final UserRefRepository userRefRepository;
    private final PermissionService permissionService;
    private final WorkspaceService workspaceService;
    private final PlatformProperties platformProperties;
    private final ApplicationEventPublisher events;

    public OrganizationServiceImpl(
            OrganizationRepository organizationRepository,
            OrganizationMemberRepository memberRepository,
            RoleRepository roleRepository,
            UserRefRepository userRefRepository,
            PermissionService permissionService,
            WorkspaceService workspaceService,
            PlatformProperties platformProperties,
            ApplicationEventPublisher events
    ) {
        this.organizationRepository = organizationRepository;
        this.memberRepository = memberRepository;
        this.roleRepository = roleRepository;
        this.userRefRepository = userRefRepository;
        this.permissionService = permissionService;
        this.workspaceService = workspaceService;
        this.platformProperties = platformProperties;
        this.events = events;
    }

    @Override
    @Transactional
    public OrganizationResponse create(UUID actorId, CreateOrganizationRequest request) {
        requireSelfServeOrMasterAdmin(actorId);
        return createInternal(actorId, request.name(), request.description(), request.slug(), actorId);
    }

    @Override
    @Transactional
    public OrganizationResponse createAsAdmin(UUID adminId, AdminCreateOrganizationRequest request) {
        requireMasterAdmin(adminId);
        if (!userRefRepository.existsByIdAndActiveTrue(request.ownerUserId())) {
            throw new ResourceNotFoundException("Owner user not found");
        }
        return createInternal(
                adminId,
                request.name(),
                request.description(),
                request.slug(),
                request.ownerUserId()
        );
    }

    private OrganizationResponse createInternal(
            UUID actorId,
            String name,
            String description,
            String slugRaw,
            UUID ownerUserId
    ) {
        String slug = slugRaw.trim().toLowerCase();
        log.info("Creating organization slug={} actor={} owner={}", slug, actorId, ownerUserId);
        if (organizationRepository.existsBySlugIgnoreCase(slug)) {
            throw new ConflictException("Organization slug already taken");
        }

        Role ownerRole = roleRepository.findById(SystemRoles.ORGANIZATION_OWNER)
                .orElseThrow(() -> new IllegalStateException("Organization Owner role not seeded"));

        Organization org = new Organization();
        org.setName(name.trim());
        org.setDescription(description);
        org.setSlug(slug);
        org.setOwnerId(ownerUserId);
        organizationRepository.save(org);

        OrganizationMember owner = new OrganizationMember();
        owner.setOrganizationId(org.getId());
        owner.setUserId(ownerUserId);
        owner.setRole(ownerRole);
        memberRepository.save(owner);

        workspaceService.createDefaultForOrganization(org.getId(), ownerUserId);

        events.publishEvent(new OrganizationCreatedDomainEvent(
                new OrganizationCreatedPayload(org.getId(), org.getName(), org.getSlug(), ownerUserId)
        ));
        events.publishEvent(new MemberAddedDomainEvent(
                new MemberAddedPayload(org.getId(), ownerUserId, ownerRole.getId(), owner.getId())
        ));

        log.info("Created organization id={} slug={} owner={}", org.getId(), slug, ownerUserId);
        return OrganizationResponse.from(org);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrganizationResponse> listMine(UUID userId) {
        log.debug("Listing organizations for userId={}", userId);
        List<OrganizationMember> memberships = memberRepository.findByUserIdOrderByJoinedAtAsc(userId);
        if (memberships.isEmpty()) {
            return List.of();
        }

        Map<UUID, Organization> orgsById = organizationRepository
                .findAllById(memberships.stream().map(OrganizationMember::getOrganizationId).toList())
                .stream()
                .filter(Organization::isActive)
                .collect(Collectors.toMap(Organization::getId, Function.identity()));

        List<OrganizationResponse> result = new ArrayList<>();
        for (OrganizationMember membership : memberships) {
            Organization org = orgsById.get(membership.getOrganizationId());
            if (org != null) {
                result.add(OrganizationResponse.from(org, membership));
            }
        }
        result.sort((a, b) -> a.name().compareToIgnoreCase(b.name()));
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public OrganizationResponse get(UUID organizationId, UUID userId) {
        log.debug("Get organization id={} userId={}", organizationId, userId);
        permissionService.requireOrgMember(organizationId, userId);
        Organization org = requireActiveOrg(organizationId);
        return memberRepository.findByOrganizationIdAndUserId(organizationId, userId)
                .map(m -> OrganizationResponse.from(org, m))
                .orElseGet(() -> OrganizationResponse.from(org));
    }

    @Override
    @Transactional
    public OrganizationResponse update(UUID organizationId, UUID userId, UpdateOrganizationRequest request) {
        log.info("Updating organization id={} userId={}", organizationId, userId);
        permissionService.requireOrgPermission(organizationId, userId, SystemRoles.ORG_UPDATE);
        Organization org = requireActiveOrg(organizationId);
        if (request.name() != null && !request.name().isBlank()) {
            org.setName(request.name().trim());
        }
        if (request.description() != null) {
            org.setDescription(request.description());
        }
        if (request.logoUrl() != null) {
            org.setLogoUrl(request.logoUrl());
        }
        events.publishEvent(new OrganizationUpdatedDomainEvent(
                new OrganizationUpdatedPayload(org.getId(), org.getName(), org.getSlug())
        ));
        log.info("Updated organization id={}", organizationId);
        return OrganizationResponse.from(org);
    }

    @Override
    @Transactional
    public void softDelete(UUID organizationId, UUID userId) {
        log.info("Soft-deleting organization id={} userId={}", organizationId, userId);
        permissionService.requireOrgPermission(organizationId, userId, SystemRoles.ORG_DELETE);
        Organization org = requireActiveOrg(organizationId);
        org.setActive(false);
        events.publishEvent(new OrganizationDeletedDomainEvent(
                new OrganizationDeletedPayload(organizationId)
        ));
        log.info("Soft-deleted organization id={}", organizationId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MemberResponse> listMembers(UUID organizationId, UUID userId) {
        log.debug("Listing members orgId={} userId={}", organizationId, userId);
        permissionService.requireOrgPermission(organizationId, userId, SystemRoles.ORG_READ);
        return memberRepository.findByOrganizationIdOrderByJoinedAtAsc(organizationId).stream()
                .map(MemberResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public MemberResponse addMember(UUID organizationId, UUID actorId, AddMemberRequest request) {
        log.info("Adding member orgId={} actor={} newUser={} roleId={}",
                organizationId, actorId, request.userId(), request.roleId());
        // Wave 4: collab invite accept forwards the invitee's JWT and calls ORG-007
        // with userId=self. Admins still need manage_members when adding someone else.
        boolean selfJoin = actorId.equals(request.userId());
        if (!selfJoin) {
            permissionService.requireOrgPermission(organizationId, actorId, SystemRoles.ORG_MANAGE_MEMBERS);
        } else if (!SystemRoles.ORGANIZATION_MEMBER.equals(request.roleId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Self-join allows Organization Member role only");
        }
        return addMemberInternal(organizationId, request);
    }

    @Override
    @Transactional
    public MemberResponse addMemberAsAdmin(UUID organizationId, UUID adminId, AddMemberRequest request) {
        log.info("Admin attach member orgId={} admin={} newUser={} roleId={}",
                organizationId, adminId, request.userId(), request.roleId());
        requireMasterAdmin(adminId);
        return addMemberInternal(organizationId, request);
    }

    private MemberResponse addMemberInternal(UUID organizationId, AddMemberRequest request) {
        Organization org = requireActiveOrg(organizationId);

        if (!userRefRepository.existsByIdAndActiveTrue(request.userId())) {
            throw new ResourceNotFoundException("User not found");
        }
        if (memberRepository.existsByOrganizationIdAndUserId(organizationId, request.userId())) {
            throw new ConflictException("User is already a member");
        }
        if (org.getMaxMembers() != null && memberRepository.countByOrganizationId(organizationId) >= org.getMaxMembers()) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Organization member limit reached");
        }

        Role role = roleRepository.findByIdAndScope(request.roleId(), "ORGANIZATION")
                .orElseThrow(() -> new ResourceNotFoundException("Organization role not found"));
        if (SystemRoles.ORGANIZATION_OWNER.equals(role.getId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Cannot assign Organization Owner via add member");
        }

        OrganizationMember member = new OrganizationMember();
        member.setOrganizationId(organizationId);
        member.setUserId(request.userId());
        member.setRole(role);
        memberRepository.save(member);

        events.publishEvent(new MemberAddedDomainEvent(
                new MemberAddedPayload(organizationId, request.userId(), role.getId(), member.getId())
        ));
        log.info("Added member id={} orgId={} userId={}", member.getId(), organizationId, request.userId());
        return MemberResponse.from(member);
    }

    @Override
    @Transactional
    public MemberResponse updateMemberRole(
            UUID organizationId,
            UUID memberId,
            UUID actorId,
            UpdateMemberRoleRequest request
    ) {
        log.info("Updating member role orgId={} memberId={} actor={} roleId={}",
                organizationId, memberId, actorId, request.roleId());
        permissionService.requireOrgPermission(organizationId, actorId, SystemRoles.ORG_MANAGE_MEMBERS);
        OrganizationMember member = memberRepository.findByIdAndOrganizationId(memberId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found"));

        Role role = roleRepository.findByIdAndScope(request.roleId(), "ORGANIZATION")
                .orElseThrow(() -> new ResourceNotFoundException("Organization role not found"));
        if (SystemRoles.ORGANIZATION_OWNER.equals(role.getId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Cannot transfer ownership via role update");
        }
        if (SystemRoles.ORGANIZATION_OWNER.equals(member.getRole().getId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Cannot change the Organization Owner role here");
        }

        member.setRole(role);
        log.info("Updated member role memberId={} roleId={}", memberId, role.getId());
        return MemberResponse.from(member);
    }

    @Override
    @Transactional
    public void removeMember(UUID organizationId, UUID memberId, UUID actorId) {
        log.info("Removing member orgId={} memberId={} actor={}", organizationId, memberId, actorId);
        permissionService.requireOrgPermission(organizationId, actorId, SystemRoles.ORG_MANAGE_MEMBERS);
        OrganizationMember member = memberRepository.findByIdAndOrganizationId(memberId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found"));
        if (SystemRoles.ORGANIZATION_OWNER.equals(member.getRole().getId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Cannot remove the Organization Owner");
        }
        UUID removedUserId = member.getUserId();
        memberRepository.delete(member);
        events.publishEvent(new MemberRemovedDomainEvent(
                new MemberRemovedPayload(organizationId, removedUserId, memberId)
        ));
        log.info("Removed member id={} orgId={}", memberId, organizationId);
    }

    @Override
    @Transactional
    public OrganizationResponse transferOwnership(
            UUID organizationId,
            UUID actorId,
            TransferOwnershipRequest request
    ) {
        Organization org = requireActiveOrg(organizationId);
        if (!actorId.equals(org.getOwnerId())) {
            throw new ForbiddenException("Only the current Organization Owner can transfer ownership");
        }
        return transferOwnershipInternal(org, actorId, request.newOwnerUserId());
    }

    @Override
    @Transactional
    public OrganizationResponse transferOwnershipAsAdmin(
            UUID organizationId,
            UUID adminId,
            TransferOwnershipRequest request
    ) {
        requireMasterAdmin(adminId);
        Organization org = requireActiveOrg(organizationId);
        return transferOwnershipInternal(org, adminId, request.newOwnerUserId());
    }

    private OrganizationResponse transferOwnershipInternal(
            Organization org,
            UUID actorId,
            UUID newOwnerUserId
    ) {
        UUID organizationId = org.getId();
        log.info("Transferring ownership orgId={} actor={} newOwner={}", organizationId, actorId, newOwnerUserId);

        if (newOwnerUserId.equals(org.getOwnerId())) {
            throw new ConflictException("User is already the Organization Owner");
        }

        OrganizationMember newOwnerMember = memberRepository
                .findByOrganizationIdAndUserId(organizationId, newOwnerUserId)
                .orElseThrow(() -> new ResourceNotFoundException("New owner must already be an organization member"));

        OrganizationMember currentOwnerMember = memberRepository
                .findByOrganizationIdAndUserId(organizationId, org.getOwnerId())
                .orElseThrow(() -> new IllegalStateException("Current owner membership missing"));

        Role ownerRole = roleRepository.findById(SystemRoles.ORGANIZATION_OWNER)
                .orElseThrow(() -> new IllegalStateException("Organization Owner role not seeded"));
        Role adminRole = roleRepository.findById(SystemRoles.ORGANIZATION_ADMIN)
                .orElseThrow(() -> new IllegalStateException("Organization Admin role not seeded"));

        currentOwnerMember.setRole(adminRole);
        newOwnerMember.setRole(ownerRole);
        org.setOwnerId(newOwnerUserId);

        events.publishEvent(new OrganizationUpdatedDomainEvent(
                new OrganizationUpdatedPayload(org.getId(), org.getName(), org.getSlug())
        ));
        log.info("Transferred ownership orgId={} newOwner={}", organizationId, newOwnerUserId);
        return OrganizationResponse.from(org);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoleResponse> listOrganizationRoles() {
        log.debug("Listing ORGANIZATION scope roles");
        return roleRepository.findByScopeOrderByNameAsc("ORGANIZATION").stream()
                .map(RoleResponse::from)
                .toList();
    }

    private void requireSelfServeOrMasterAdmin(UUID actorId) {
        if (platformProperties.isSelfServeOrgCreate()) {
            return;
        }
        requireMasterAdmin(actorId);
    }

    private void requireMasterAdmin(UUID userId) {
        if (!userRefRepository.existsByIdAndActiveTrueAndMasterAdminTrue(userId)) {
            throw new ForbiddenException("Master admin required");
        }
    }

    private Organization requireActiveOrg(UUID organizationId) {
        return organizationRepository.findByIdAndActiveTrue(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));
    }
}
