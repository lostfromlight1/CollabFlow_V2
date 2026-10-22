package com.collabflow.platform.service.impl;

import com.collabflow.common.exception.BusinessException;
import com.collabflow.common.exception.ConflictException;
import com.collabflow.common.exception.ErrorCode;
import com.collabflow.common.exception.ResourceNotFoundException;
import com.collabflow.platform.dto.request.AddWorkspaceMemberRequest;
import com.collabflow.platform.dto.request.CreateWorkspaceRequest;
import com.collabflow.platform.dto.request.JoinWorkspaceRequest;
import com.collabflow.platform.dto.request.UpdateWorkspaceMemberRoleRequest;
import com.collabflow.platform.dto.request.UpdateWorkspaceRequest;
import com.collabflow.platform.dto.response.RoleResponse;
import com.collabflow.platform.dto.response.WorkspaceMemberResponse;
import com.collabflow.platform.dto.response.WorkspaceResponse;
import com.collabflow.platform.event.WorkspaceCreatedDomainEvent;
import com.collabflow.platform.event.WorkspaceCreatedPayload;
import com.collabflow.platform.event.WorkspaceDeletedDomainEvent;
import com.collabflow.platform.event.WorkspaceDeletedPayload;
import com.collabflow.platform.event.WorkspaceMemberAddedDomainEvent;
import com.collabflow.platform.event.WorkspaceMemberAddedPayload;
import com.collabflow.platform.event.WorkspaceMemberRemovedDomainEvent;
import com.collabflow.platform.event.WorkspaceMemberRemovedPayload;
import com.collabflow.platform.event.WorkspaceUpdatedDomainEvent;
import com.collabflow.platform.event.WorkspaceUpdatedPayload;
import com.collabflow.platform.model.Role;
import com.collabflow.platform.model.Workspace;
import com.collabflow.platform.model.WorkspaceMember;
import com.collabflow.platform.repository.OrganizationRepository;
import com.collabflow.platform.repository.RoleRepository;
import com.collabflow.platform.repository.WorkspaceMemberRepository;
import com.collabflow.platform.repository.WorkspaceRepository;
import com.collabflow.platform.security.SystemRoles;
import com.collabflow.platform.service.PermissionService;
import com.collabflow.platform.service.WorkspaceService;
import java.security.SecureRandom;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkspaceServiceImpl implements WorkspaceService {

    private static final Logger log = LoggerFactory.getLogger(WorkspaceServiceImpl.class);
    private static final String INVITE_ALPHABET = "abcdefghijklmnopqrstuvwxyz0123456789";
    private static final int INVITE_LENGTH = 10;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository memberRepository;
    private final OrganizationRepository organizationRepository;
    private final RoleRepository roleRepository;
    private final PermissionService permissionService;
    private final ApplicationEventPublisher events;

    public WorkspaceServiceImpl(
            WorkspaceRepository workspaceRepository,
            WorkspaceMemberRepository memberRepository,
            OrganizationRepository organizationRepository,
            RoleRepository roleRepository,
            PermissionService permissionService,
            ApplicationEventPublisher events
    ) {
        this.workspaceRepository = workspaceRepository;
        this.memberRepository = memberRepository;
        this.organizationRepository = organizationRepository;
        this.roleRepository = roleRepository;
        this.permissionService = permissionService;
        this.events = events;
    }

    @Override
    @Transactional
    public WorkspaceResponse create(UUID actorId, CreateWorkspaceRequest request) {
        UUID organizationId = request.organizationId();
        log.info("Creating workspace orgId={} actor={}", organizationId, actorId);

        organizationRepository.findByIdAndActiveTrue(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));
        permissionService.requireOrgPermission(organizationId, actorId, SystemRoles.WORKSPACE_CREATE);

        Role adminRole = roleRepository.findById(SystemRoles.WORKSPACE_ADMIN)
                .orElseThrow(() -> new IllegalStateException("Workspace Admin role not seeded"));

        Workspace workspace = new Workspace();
        workspace.setName(request.name().trim());
        workspace.setDescription(request.description());
        workspace.setOrganizationId(organizationId);
        workspace.setOwnerId(actorId);
        workspace.setInviteCode(generateUniqueInviteCode());
        if (request.timeZone() != null && !request.timeZone().isBlank()) {
            workspace.setTimeZone(request.timeZone().trim());
        }
        if (request.dateFormat() != null && !request.dateFormat().isBlank()) {
            workspace.setDateFormat(request.dateFormat().trim());
        }
        workspaceRepository.save(workspace);

        WorkspaceMember owner = new WorkspaceMember();
        owner.setWorkspaceId(workspace.getId());
        owner.setUserId(actorId);
        owner.setRole(adminRole);
        memberRepository.save(owner);

        events.publishEvent(new WorkspaceCreatedDomainEvent(new WorkspaceCreatedPayload(
                workspace.getId(), organizationId, workspace.getName(), actorId
        )));
        events.publishEvent(new WorkspaceMemberAddedDomainEvent(new WorkspaceMemberAddedPayload(
                workspace.getId(), organizationId, actorId, adminRole.getId(), owner.getId()
        )));

        log.info("Created workspace id={} orgId={} owner={}", workspace.getId(), organizationId, actorId);
        return WorkspaceResponse.from(workspace);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkspaceResponse> listByOrganization(UUID organizationId, UUID userId) {
        log.debug("Listing workspaces orgId={} userId={}", organizationId, userId);
        organizationRepository.findByIdAndActiveTrue(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));
        permissionService.requireOrgMember(organizationId, userId);
        return workspaceRepository.findByOrganizationIdAndActiveTrueOrderByNameAsc(organizationId).stream()
                .map(WorkspaceResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public WorkspaceResponse get(UUID workspaceId, UUID userId) {
        log.debug("Get workspace id={} userId={}", workspaceId, userId);
        permissionService.requireWorkspaceMember(workspaceId, userId);
        return WorkspaceResponse.from(requireActiveWorkspace(workspaceId));
    }

    @Override
    @Transactional
    public WorkspaceResponse update(UUID workspaceId, UUID userId, UpdateWorkspaceRequest request) {
        log.info("Updating workspace id={} userId={}", workspaceId, userId);
        permissionService.requireWorkspacePermission(workspaceId, userId, SystemRoles.WORKSPACE_UPDATE);
        Workspace workspace = requireActiveWorkspace(workspaceId);
        if (request.name() != null && !request.name().isBlank()) {
            workspace.setName(request.name().trim());
        }
        if (request.description() != null) {
            workspace.setDescription(request.description());
        }
        if (request.imageUrl() != null) {
            workspace.setImageUrl(request.imageUrl());
        }
        if (request.timeZone() != null && !request.timeZone().isBlank()) {
            workspace.setTimeZone(request.timeZone().trim());
        }
        if (request.dateFormat() != null && !request.dateFormat().isBlank()) {
            workspace.setDateFormat(request.dateFormat().trim());
        }
        events.publishEvent(new WorkspaceUpdatedDomainEvent(new WorkspaceUpdatedPayload(
                workspace.getId(), workspace.getOrganizationId(), workspace.getName()
        )));
        log.info("Updated workspace id={}", workspaceId);
        return WorkspaceResponse.from(workspace);
    }

    @Override
    @Transactional
    public void softDelete(UUID workspaceId, UUID userId) {
        log.info("Soft-deleting workspace id={} userId={}", workspaceId, userId);
        permissionService.requireWorkspacePermission(workspaceId, userId, SystemRoles.WORKSPACE_DELETE);
        Workspace workspace = requireActiveWorkspace(workspaceId);
        workspace.setActive(false);
        events.publishEvent(new WorkspaceDeletedDomainEvent(new WorkspaceDeletedPayload(
                workspace.getId(), workspace.getOrganizationId()
        )));
        log.info("Soft-deleted workspace id={}", workspaceId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkspaceMemberResponse> listMembers(UUID workspaceId, UUID userId) {
        log.debug("Listing members workspaceId={} userId={}", workspaceId, userId);
        permissionService.requireWorkspacePermission(workspaceId, userId, SystemRoles.WORKSPACE_READ);
        return memberRepository.findByWorkspaceIdOrderByJoinedAtAsc(workspaceId).stream()
                .map(WorkspaceMemberResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public WorkspaceMemberResponse addMember(UUID workspaceId, UUID actorId, AddWorkspaceMemberRequest request) {
        log.info("Adding workspace member ws={} actor={} user={} role={}",
                workspaceId, actorId, request.userId(), request.roleId());
        permissionService.requireWorkspacePermission(workspaceId, actorId, SystemRoles.WORKSPACE_MANAGE_MEMBERS);
        Workspace workspace = requireActiveWorkspace(workspaceId);
        permissionService.requireOrgMember(workspace.getOrganizationId(), request.userId());

        if (memberRepository.existsByWorkspaceIdAndUserId(workspaceId, request.userId())) {
            throw new ConflictException("User is already a workspace member");
        }

        Role role = roleRepository.findByIdAndScope(request.roleId(), "WORKSPACE")
                .orElseThrow(() -> new ResourceNotFoundException("Workspace role not found"));

        WorkspaceMember member = new WorkspaceMember();
        member.setWorkspaceId(workspaceId);
        member.setUserId(request.userId());
        member.setRole(role);
        memberRepository.save(member);

        events.publishEvent(new WorkspaceMemberAddedDomainEvent(new WorkspaceMemberAddedPayload(
                workspaceId, workspace.getOrganizationId(), request.userId(), role.getId(), member.getId()
        )));
        log.info("Added workspace member id={} ws={}", member.getId(), workspaceId);
        return WorkspaceMemberResponse.from(member);
    }

    @Override
    @Transactional
    public WorkspaceMemberResponse updateMemberRole(
            UUID workspaceId,
            UUID memberId,
            UUID actorId,
            UpdateWorkspaceMemberRoleRequest request
    ) {
        log.info("Updating workspace member role ws={} memberId={} actor={}", workspaceId, memberId, actorId);
        permissionService.requireWorkspacePermission(workspaceId, actorId, SystemRoles.WORKSPACE_MANAGE_MEMBERS);
        Workspace workspace = requireActiveWorkspace(workspaceId);
        WorkspaceMember member = memberRepository.findByIdAndWorkspaceId(memberId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found"));

        if (workspace.getOwnerId().equals(member.getUserId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Cannot change the workspace owner role here");
        }

        Role role = roleRepository.findByIdAndScope(request.roleId(), "WORKSPACE")
                .orElseThrow(() -> new ResourceNotFoundException("Workspace role not found"));
        member.setRole(role);
        log.info("Updated workspace member role memberId={} roleId={}", memberId, role.getId());
        return WorkspaceMemberResponse.from(member);
    }

    @Override
    @Transactional
    public void removeMember(UUID workspaceId, UUID memberId, UUID actorId) {
        log.info("Removing workspace member ws={} memberId={} actor={}", workspaceId, memberId, actorId);
        permissionService.requireWorkspacePermission(workspaceId, actorId, SystemRoles.WORKSPACE_MANAGE_MEMBERS);
        Workspace workspace = requireActiveWorkspace(workspaceId);
        WorkspaceMember member = memberRepository.findByIdAndWorkspaceId(memberId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found"));
        if (workspace.getOwnerId().equals(member.getUserId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Cannot remove the workspace owner");
        }
        memberRepository.delete(member);
        events.publishEvent(new WorkspaceMemberRemovedDomainEvent(new WorkspaceMemberRemovedPayload(
                workspaceId, workspace.getOrganizationId(), member.getUserId(), memberId
        )));
        log.info("Removed workspace member id={} ws={}", memberId, workspaceId);
    }

    @Override
    @Transactional
    public WorkspaceMemberResponse join(UUID workspaceId, UUID userId, JoinWorkspaceRequest request) {
        log.info("Join workspace id={} userId={}", workspaceId, userId);
        Workspace workspace = requireActiveWorkspace(workspaceId);
        permissionService.requireOrgMember(workspace.getOrganizationId(), userId);

        String code = request.inviteCode().trim().toLowerCase(Locale.ROOT);
        if (!workspace.getInviteCode().equalsIgnoreCase(code)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Invalid invite code");
        }
        if (memberRepository.existsByWorkspaceIdAndUserId(workspaceId, userId)) {
            throw new ConflictException("User is already a workspace member");
        }

        Role memberRole = roleRepository.findById(SystemRoles.WORKSPACE_MEMBER)
                .orElseThrow(() -> new IllegalStateException("Workspace Member role not seeded"));

        WorkspaceMember member = new WorkspaceMember();
        member.setWorkspaceId(workspaceId);
        member.setUserId(userId);
        member.setRole(memberRole);
        memberRepository.save(member);

        events.publishEvent(new WorkspaceMemberAddedDomainEvent(new WorkspaceMemberAddedPayload(
                workspaceId, workspace.getOrganizationId(), userId, memberRole.getId(), member.getId()
        )));
        log.info("User {} joined workspace {}", userId, workspaceId);
        return WorkspaceMemberResponse.from(member);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoleResponse> listWorkspaceRoles() {
        return roleRepository.findByScopeOrderByNameAsc("WORKSPACE").stream()
                .map(RoleResponse::from)
                .toList();
    }

    private Workspace requireActiveWorkspace(UUID workspaceId) {
        return workspaceRepository.findByIdAndActiveTrue(workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found"));
    }

    private String generateUniqueInviteCode() {
        for (int attempt = 0; attempt < 20; attempt++) {
            String code = randomInviteCode();
            if (!workspaceRepository.existsByInviteCodeIgnoreCase(code)) {
                return code;
            }
        }
        throw new IllegalStateException("Could not generate unique invite code");
    }

    private static String randomInviteCode() {
        StringBuilder sb = new StringBuilder(INVITE_LENGTH);
        for (int i = 0; i < INVITE_LENGTH; i++) {
            sb.append(INVITE_ALPHABET.charAt(RANDOM.nextInt(INVITE_ALPHABET.length())));
        }
        return sb.toString();
    }
}
