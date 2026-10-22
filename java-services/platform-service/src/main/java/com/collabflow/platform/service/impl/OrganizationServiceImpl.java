package com.collabflow.platform.service.impl;

import com.collabflow.common.exception.BusinessException;
import com.collabflow.common.exception.ConflictException;
import com.collabflow.common.exception.ErrorCode;
import com.collabflow.common.exception.ResourceNotFoundException;
import com.collabflow.platform.dto.request.AddMemberRequest;
import com.collabflow.platform.dto.request.CreateOrganizationRequest;
import com.collabflow.platform.dto.request.UpdateMemberRoleRequest;
import com.collabflow.platform.dto.request.UpdateOrganizationRequest;
import com.collabflow.platform.dto.response.MemberResponse;
import com.collabflow.platform.dto.response.OrganizationResponse;
import com.collabflow.platform.dto.response.RoleResponse;
import com.collabflow.platform.event.MemberAddedDomainEvent;
import com.collabflow.platform.event.MemberAddedPayload;
import com.collabflow.platform.event.OrganizationCreatedDomainEvent;
import com.collabflow.platform.event.OrganizationCreatedPayload;
import com.collabflow.platform.model.Organization;
import com.collabflow.platform.model.OrganizationMember;
import com.collabflow.platform.model.Role;
import com.collabflow.platform.repository.OrganizationMemberRepository;
import com.collabflow.platform.repository.OrganizationRepository;
import com.collabflow.platform.repository.RoleRepository;
import com.collabflow.platform.security.SystemRoles;
import com.collabflow.platform.service.OrganizationService;
import com.collabflow.platform.service.PermissionService;
import java.util.List;
import java.util.UUID;
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
    private final PermissionService permissionService;
    private final ApplicationEventPublisher events;

    public OrganizationServiceImpl(
            OrganizationRepository organizationRepository,
            OrganizationMemberRepository memberRepository,
            RoleRepository roleRepository,
            PermissionService permissionService,
            ApplicationEventPublisher events
    ) {
        this.organizationRepository = organizationRepository;
        this.memberRepository = memberRepository;
        this.roleRepository = roleRepository;
        this.permissionService = permissionService;
        this.events = events;
    }

    @Override
    @Transactional
    public OrganizationResponse create(UUID actorId, CreateOrganizationRequest request) {
        String slug = request.slug().trim().toLowerCase();
        log.info("Creating organization slug={} actor={}", slug, actorId);
        if (organizationRepository.existsBySlugIgnoreCase(slug)) {
            throw new ConflictException("Organization slug already taken");
        }

        Role ownerRole = roleRepository.findById(SystemRoles.ORGANIZATION_OWNER)
                .orElseThrow(() -> new IllegalStateException("Organization Owner role not seeded"));

        Organization org = new Organization();
        org.setName(request.name().trim());
        org.setDescription(request.description());
        org.setSlug(slug);
        org.setOwnerId(actorId);
        organizationRepository.save(org);

        OrganizationMember owner = new OrganizationMember();
        owner.setOrganizationId(org.getId());
        owner.setUserId(actorId);
        owner.setRole(ownerRole);
        memberRepository.save(owner);

        events.publishEvent(new OrganizationCreatedDomainEvent(
                new OrganizationCreatedPayload(org.getId(), org.getName(), org.getSlug(), actorId)
        ));
        events.publishEvent(new MemberAddedDomainEvent(
                new MemberAddedPayload(org.getId(), actorId, ownerRole.getId(), owner.getId())
        ));

        log.info("Created organization id={} slug={} owner={}", org.getId(), slug, actorId);
        return OrganizationResponse.from(org);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrganizationResponse> listMine(UUID userId) {
        log.debug("Listing organizations for userId={}", userId);
        return organizationRepository.findActiveForUser(userId).stream()
                .map(OrganizationResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public OrganizationResponse get(UUID organizationId, UUID userId) {
        log.debug("Get organization id={} userId={}", organizationId, userId);
        permissionService.requireOrgMember(organizationId, userId);
        Organization org = requireActiveOrg(organizationId);
        return OrganizationResponse.from(org);
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
        permissionService.requireOrgPermission(organizationId, actorId, SystemRoles.ORG_MANAGE_MEMBERS);
        Organization org = requireActiveOrg(organizationId);

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
        memberRepository.delete(member);
        log.info("Removed member id={} orgId={}", memberId, organizationId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoleResponse> listOrganizationRoles() {
        log.debug("Listing ORGANIZATION scope roles");
        return roleRepository.findByScopeOrderByNameAsc("ORGANIZATION").stream()
                .map(RoleResponse::from)
                .toList();
    }

    private Organization requireActiveOrg(UUID organizationId) {
        return organizationRepository.findByIdAndActiveTrue(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));
    }
}
