package com.collabflow.platform.service.impl;

import com.collabflow.common.exception.BusinessException;
import com.collabflow.common.exception.ConflictException;
import com.collabflow.common.exception.ErrorCode;
import com.collabflow.common.exception.ResourceNotFoundException;
import com.collabflow.platform.dto.request.AddProjectMemberRequest;
import com.collabflow.platform.dto.request.CreateProjectRequest;
import com.collabflow.platform.dto.request.UpdateProjectMemberRoleRequest;
import com.collabflow.platform.dto.request.UpdateProjectRequest;
import com.collabflow.platform.dto.response.ProjectMemberResponse;
import com.collabflow.platform.dto.response.ProjectResponse;
import com.collabflow.platform.dto.response.RoleResponse;
import com.collabflow.platform.event.ProjectCreatedDomainEvent;
import com.collabflow.platform.event.ProjectCreatedPayload;
import com.collabflow.platform.event.ProjectDeletedDomainEvent;
import com.collabflow.platform.event.ProjectDeletedPayload;
import com.collabflow.platform.event.ProjectMemberAddedDomainEvent;
import com.collabflow.platform.event.ProjectMemberAddedPayload;
import com.collabflow.platform.event.ProjectMemberRemovedDomainEvent;
import com.collabflow.platform.event.ProjectMemberRemovedPayload;
import com.collabflow.platform.event.ProjectUpdatedDomainEvent;
import com.collabflow.platform.event.ProjectUpdatedPayload;
import com.collabflow.platform.model.Project;
import com.collabflow.platform.model.ProjectMember;
import com.collabflow.platform.model.Role;
import com.collabflow.platform.model.Workspace;
import com.collabflow.platform.repository.ProjectMemberRepository;
import com.collabflow.platform.repository.ProjectRepository;
import com.collabflow.platform.repository.RoleRepository;
import com.collabflow.platform.repository.WorkspaceRepository;
import com.collabflow.platform.security.SystemRoles;
import com.collabflow.platform.service.PermissionService;
import com.collabflow.platform.service.ProjectService;
import com.collabflow.platform.service.UserDisplayLookup;
import com.collabflow.platform.service.UserDisplayLookup.UserDisplay;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectServiceImpl implements ProjectService {

    private static final Logger log = LoggerFactory.getLogger(ProjectServiceImpl.class);
    private static final Set<String> ALLOWED_TYPES = Set.of(
            Project.TYPE_SOFTWARE, Project.TYPE_MARKETING, Project.TYPE_HR, Project.TYPE_CUSTOM
    );
    private static final Set<String> ALLOWED_STATUSES = Set.of(
            Project.STATUS_ACTIVE, Project.STATUS_ARCHIVED, Project.STATUS_ON_HOLD
    );

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository memberRepository;
    private final WorkspaceRepository workspaceRepository;
    private final RoleRepository roleRepository;
    private final UserDisplayLookup userDisplayLookup;
    private final PermissionService permissionService;
    private final ApplicationEventPublisher events;

    public ProjectServiceImpl(
            ProjectRepository projectRepository,
            ProjectMemberRepository memberRepository,
            WorkspaceRepository workspaceRepository,
            RoleRepository roleRepository,
            UserDisplayLookup userDisplayLookup,
            PermissionService permissionService,
            ApplicationEventPublisher events
    ) {
        this.projectRepository = projectRepository;
        this.memberRepository = memberRepository;
        this.workspaceRepository = workspaceRepository;
        this.roleRepository = roleRepository;
        this.userDisplayLookup = userDisplayLookup;
        this.permissionService = permissionService;
        this.events = events;
    }

    @Override
    @Transactional
    public ProjectResponse create(UUID actorId, CreateProjectRequest request) {
        UUID workspaceId = request.workspaceId();
        log.info("Creating project workspaceId={} actor={}", workspaceId, actorId);

        requireActiveWorkspace(workspaceId);
        permissionService.requireWorkspacePermission(workspaceId, actorId, SystemRoles.PROJECT_CREATE);

        if (request.parentProjectId() != null) {
            Project parent = projectRepository.findByIdAndStatusNot(request.parentProjectId(), Project.STATUS_ARCHIVED)
                    .orElseThrow(() -> new ResourceNotFoundException("Parent project not found"));
            if (!parent.getWorkspaceId().equals(workspaceId)) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Parent project must be in the same workspace");
            }
            if (!Project.STATUS_ACTIVE.equals(parent.getStatus())) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Parent project must be ACTIVE");
            }
        }

        Role adminRole = roleRepository.findById(SystemRoles.PROJECT_ADMIN)
                .orElseThrow(() -> new IllegalStateException("Project Admin role not seeded"));

        Project project = new Project();
        project.setName(request.name().trim());
        project.setDescription(request.description());
        project.setWorkspaceId(workspaceId);
        project.setOwnerId(actorId);
        project.setParentProjectId(request.parentProjectId());
        project.setLeadId(request.leadId() != null ? request.leadId() : actorId);
        project.setStartDate(request.startDate());
        project.setDueDate(request.dueDate());
        if (request.projectType() != null && !request.projectType().isBlank()) {
            project.setProjectType(normalizeType(request.projectType()));
        }
        projectRepository.save(project);

        ProjectMember owner = new ProjectMember();
        owner.setProjectId(project.getId());
        owner.setUserId(actorId);
        owner.setRole(adminRole);
        memberRepository.save(owner);

        events.publishEvent(new ProjectCreatedDomainEvent(new ProjectCreatedPayload(
                project.getId(), workspaceId, project.getName(), actorId
        )));
        events.publishEvent(new ProjectMemberAddedDomainEvent(new ProjectMemberAddedPayload(
                project.getId(), workspaceId, actorId, adminRole.getId(), owner.getId()
        )));

        log.info("Created project id={} workspaceId={} owner={}", project.getId(), workspaceId, actorId);
        return ProjectResponse.from(project);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectResponse> listByWorkspace(UUID workspaceId, UUID userId) {
        log.debug("Listing projects workspaceId={} userId={}", workspaceId, userId);
        requireActiveWorkspace(workspaceId);
        permissionService.requireWorkspaceMember(workspaceId, userId);
        return projectRepository.findByWorkspaceIdAndStatusOrderByNameAsc(workspaceId, Project.STATUS_ACTIVE).stream()
                .map(ProjectResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectResponse get(UUID projectId, UUID userId) {
        log.debug("Get project id={} userId={}", projectId, userId);
        permissionService.requireProjectMember(projectId, userId);
        return ProjectResponse.from(requireActiveProject(projectId));
    }

    @Override
    @Transactional
    public ProjectResponse update(UUID projectId, UUID userId, UpdateProjectRequest request) {
        log.info("Updating project id={} userId={}", projectId, userId);
        permissionService.requireProjectPermission(projectId, userId, SystemRoles.PROJECT_UPDATE);
        Project project = requireActiveProject(projectId);

        if (request.name() != null && !request.name().isBlank()) {
            project.setName(request.name().trim());
        }
        if (request.description() != null) {
            project.setDescription(request.description());
        }
        if (request.imageUrl() != null) {
            project.setImageUrl(request.imageUrl());
        }
        if (request.projectType() != null && !request.projectType().isBlank()) {
            project.setProjectType(normalizeType(request.projectType()));
        }
        if (request.status() != null && !request.status().isBlank()) {
            String status = request.status().trim().toUpperCase(Locale.ROOT);
            if (!ALLOWED_STATUSES.contains(status) || Project.STATUS_ARCHIVED.equals(status)) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Use DELETE to archive; status must be ACTIVE or ON_HOLD");
            }
            project.setStatus(status);
        }
        if (request.leadId() != null) {
            project.setLeadId(request.leadId());
        }
        if (request.startDate() != null) {
            project.setStartDate(request.startDate());
        }
        if (request.dueDate() != null) {
            project.setDueDate(request.dueDate());
        }

        events.publishEvent(new ProjectUpdatedDomainEvent(new ProjectUpdatedPayload(
                project.getId(), project.getWorkspaceId(), project.getName()
        )));
        log.info("Updated project id={}", projectId);
        return ProjectResponse.from(project);
    }

    @Override
    @Transactional
    public void archive(UUID projectId, UUID userId) {
        log.info("Archiving project id={} userId={}", projectId, userId);
        permissionService.requireProjectPermission(projectId, userId, SystemRoles.PROJECT_DELETE);
        Project project = requireActiveProject(projectId);
        project.setStatus(Project.STATUS_ARCHIVED);
        events.publishEvent(new ProjectDeletedDomainEvent(new ProjectDeletedPayload(
                project.getId(), project.getWorkspaceId()
        )));
        log.info("Archived project id={}", projectId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectMemberResponse> listMembers(UUID projectId, UUID userId) {
        log.debug("Listing members projectId={} userId={}", projectId, userId);
        permissionService.requireProjectPermission(projectId, userId, SystemRoles.PROJECT_READ);
        List<ProjectMember> members = memberRepository.findByProjectIdOrderByJoinedAtAsc(projectId);
        Map<UUID, UserDisplay> profiles = userDisplayLookup.findByIds(
                members.stream().map(ProjectMember::getUserId).toList()
        );
        return members.stream().map(m -> view(m, profiles.get(m.getUserId()))).toList();
    }

    @Override
    @Transactional
    public ProjectMemberResponse addMember(UUID projectId, UUID actorId, AddProjectMemberRequest request) {
        log.info("Adding project member project={} actor={} user={} role={}",
                projectId, actorId, request.userId(), request.roleId());
        // Wave 4: collab invite accept may self-join after workspace membership is created.
        boolean selfJoin = actorId.equals(request.userId());
        if (!selfJoin) {
            permissionService.requireProjectPermission(projectId, actorId, SystemRoles.PROJECT_MANAGE_MEMBERS);
        } else if (!SystemRoles.PROJECT_MEMBER.equals(request.roleId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Self-join allows Project Member role only");
        }
        Project project = requireActiveProject(projectId);
        permissionService.requireWorkspaceMember(project.getWorkspaceId(), request.userId());

        if (memberRepository.existsByProjectIdAndUserId(projectId, request.userId())) {
            throw new ConflictException("User is already a project member");
        }

        Role role = roleRepository.findByIdAndScope(request.roleId(), "PROJECT")
                .orElseThrow(() -> new ResourceNotFoundException("Project role not found"));

        ProjectMember member = new ProjectMember();
        member.setProjectId(projectId);
        member.setUserId(request.userId());
        member.setRole(role);
        memberRepository.save(member);

        events.publishEvent(new ProjectMemberAddedDomainEvent(new ProjectMemberAddedPayload(
                projectId, project.getWorkspaceId(), request.userId(), role.getId(), member.getId()
        )));
        log.info("Added project member id={} project={}", member.getId(), projectId);
        return view(member);
    }

    @Override
    @Transactional
    public ProjectMemberResponse updateMemberRole(
            UUID projectId,
            UUID memberId,
            UUID actorId,
            UpdateProjectMemberRoleRequest request
    ) {
        log.info("Updating project member role project={} memberId={} actor={}", projectId, memberId, actorId);
        permissionService.requireProjectPermission(projectId, actorId, SystemRoles.PROJECT_MANAGE_MEMBERS);
        Project project = requireActiveProject(projectId);
        ProjectMember member = memberRepository.findByIdAndProjectId(memberId, projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found"));

        if (project.getOwnerId().equals(member.getUserId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Cannot change the project owner role here");
        }

        Role role = roleRepository.findByIdAndScope(request.roleId(), "PROJECT")
                .orElseThrow(() -> new ResourceNotFoundException("Project role not found"));
        member.setRole(role);
        log.info("Updated project member role memberId={} roleId={}", memberId, role.getId());
        return view(member);
    }

    @Override
    @Transactional
    public void removeMember(UUID projectId, UUID memberId, UUID actorId) {
        log.info("Removing project member project={} memberId={} actor={}", projectId, memberId, actorId);
        permissionService.requireProjectPermission(projectId, actorId, SystemRoles.PROJECT_MANAGE_MEMBERS);
        Project project = requireActiveProject(projectId);
        ProjectMember member = memberRepository.findByIdAndProjectId(memberId, projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found"));
        if (project.getOwnerId().equals(member.getUserId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Cannot remove the project owner");
        }
        memberRepository.delete(member);
        events.publishEvent(new ProjectMemberRemovedDomainEvent(new ProjectMemberRemovedPayload(
                projectId, project.getWorkspaceId(), member.getUserId(), memberId
        )));
        log.info("Removed project member id={} project={}", memberId, projectId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoleResponse> listProjectRoles() {
        return roleRepository.findByScopeOrderByNameAsc("PROJECT").stream()
                .map(RoleResponse::from)
                .toList();
    }

    private Workspace requireActiveWorkspace(UUID workspaceId) {
        return workspaceRepository.findByIdAndActiveTrue(workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found"));
    }

    private Project requireActiveProject(UUID projectId) {
        return projectRepository.findByIdAndStatusNot(projectId, Project.STATUS_ARCHIVED)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
    }

    private ProjectMemberResponse view(ProjectMember member) {
        return view(member, userDisplayLookup.find(member.getUserId()));
    }

    private ProjectMemberResponse view(ProjectMember member, UserDisplay profile) {
        if (profile == null) {
            return ProjectMemberResponse.from(member);
        }
        return ProjectMemberResponse.from(member, profile.name(), profile.email());
    }

    private static String normalizeType(String projectType) {
        String type = projectType.trim().toUpperCase(Locale.ROOT);
        if (!ALLOWED_TYPES.contains(type)) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Invalid projectType: " + projectType);
        }
        return type;
    }
}
