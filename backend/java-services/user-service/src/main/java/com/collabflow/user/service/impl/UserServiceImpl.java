package com.collabflow.user.service.impl;

import com.collabflow.common.api.PageResponse;
import com.collabflow.common.exception.BusinessException;
import com.collabflow.common.exception.ConflictException;
import com.collabflow.common.exception.ErrorCode;
import com.collabflow.common.exception.ForbiddenException;
import com.collabflow.common.exception.ResourceNotFoundException;
import com.collabflow.user.dto.request.AdminCreateUserRequest;
import com.collabflow.user.dto.request.SetMasterAdminRequest;
import com.collabflow.user.dto.request.UpdateProfileRequest;
import com.collabflow.user.dto.response.AdminUserResponse;
import com.collabflow.user.dto.response.UserResponse;
import com.collabflow.user.event.EmailSendDomainEvent;
import com.collabflow.user.event.EmailSendPayload;
import com.collabflow.user.event.UserCreatedDomainEvent;
import com.collabflow.user.event.UserCreatedPayload;
import com.collabflow.user.event.UserUpdatedDomainEvent;
import com.collabflow.user.event.UserUpdatedPayload;
import com.collabflow.user.model.User;
import com.collabflow.user.repository.UserRepository;
import com.collabflow.user.service.AdminAuditWriter;
import com.collabflow.user.service.AdminUserDirectory;
import com.collabflow.user.service.AuthSessionRevoker;
import com.collabflow.user.service.UserService;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserServiceImpl implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;
    private final AdminUserDirectory adminUserDirectory;
    private final AdminAuditWriter auditWriter;
    private final AuthSessionRevoker authSessionRevoker;

    public UserServiceImpl(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            ApplicationEventPublisher eventPublisher,
            AdminUserDirectory adminUserDirectory,
            AdminAuditWriter auditWriter,
            AuthSessionRevoker authSessionRevoker
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.eventPublisher = eventPublisher;
        this.adminUserDirectory = adminUserDirectory;
        this.auditWriter = auditWriter;
        this.authSessionRevoker = authSessionRevoker;
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse me(UUID userId) {
        return toResponse(requireUser(userId));
    }

    @Override
    @Transactional
    public UserResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        User user = requireUser(userId);
        if (request.name() != null && !request.name().isBlank()) {
            user.setName(request.name().trim());
        }
        if (request.avatarUrl() != null) {
            user.setAvatarUrl(request.avatarUrl().isBlank() ? null : request.avatarUrl().trim());
        }
        User saved = userRepository.save(user);
        eventPublisher.publishEvent(new UserUpdatedDomainEvent(
                new UserUpdatedPayload(saved.getId(), saved.getEmail(), saved.getName())
        ));
        return toResponse(saved);
    }

    @Override
    @Transactional
    public UserResponse adminCreateUser(UUID adminUserId, AdminCreateUserRequest request) {
        User admin = requireUser(adminUserId);
        if (!admin.isMasterAdmin()) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Master admin required");
        }

        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("Email already registered");
        }

        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.temporaryPassword()));
        user.setName(request.name().trim());
        user.setMasterAdmin(false);
        // Admin-provisioned: verified so login works with the temp password.
        // Welcome mail still goes out via email.send → email-service SMTP (MailHog locally).
        // Auth owns verify/reset tokens for public register / forgot-password.
        user.setEmailVerified(true);
        userRepository.save(user);
        log.info("Admin created user id={} email={} by={}", user.getId(), email, adminUserId);

        eventPublisher.publishEvent(new UserCreatedDomainEvent(
                new UserCreatedPayload(user.getId(), user.getEmail(), user.getName())
        ));
        eventPublisher.publishEvent(new EmailSendDomainEvent(EmailSendPayload.of(
                user.getEmail(),
                "Welcome to CollabFlow",
                "Your account was created by an admin. Email: " + user.getEmail()
                        + " — sign in with the temporary password you were given, then change it.",
                "welcome"
        )));
        auditWriter.write(adminUserId, "USER_CREATED", "USER", user.getId(), null);
        return toResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AdminUserResponse> adminList(
            UUID adminId,
            String q,
            Boolean active,
            Boolean masterAdmin,
            Boolean emailVerified,
            UUID organizationId,
            Instant createdFrom,
            Instant createdTo,
            String deleted,
            int page,
            int size,
            String sort
    ) {
        requireMasterAdmin(adminId);
        return adminUserDirectory.list(
                q, active, masterAdmin, emailVerified, organizationId,
                createdFrom, createdTo, deleted, page, size, sort
        );
    }

    @Override
    @Transactional(readOnly = true)
    public AdminUserResponse adminGet(UUID adminId, UUID userId) {
        requireMasterAdmin(adminId);
        User user = requireUser(userId);
        return toAdmin(user);
    }

    @Override
    @Transactional(readOnly = true)
    public String adminExport(
            UUID adminId,
            String q,
            Boolean active,
            Boolean masterAdmin,
            Boolean emailVerified,
            UUID organizationId,
            Instant createdFrom,
            Instant createdTo,
            String deleted,
            String sort
    ) {
        requireMasterAdmin(adminId);
        return adminUserDirectory.exportCsv(
                q, active, masterAdmin, emailVerified, organizationId,
                createdFrom, createdTo, deleted, sort
        );
    }

    @Override
    @Transactional
    public void setMasterAdmin(UUID adminId, UUID userId, SetMasterAdminRequest request) {
        requireMasterAdmin(adminId);
        if (adminId.equals(userId) && !request.masterAdmin()) {
            throw new ForbiddenException("You cannot remove your own master admin flag");
        }
        User user = requireUser(userId);
        if (user.getDeletedAt() != null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Restore the user first");
        }
        user.setMasterAdmin(request.masterAdmin());
        userRepository.save(user);
        auditWriter.write(
                adminId,
                "USER_MASTER_ADMIN_CHANGED",
                "USER",
                userId,
                "{\"masterAdmin\":" + request.masterAdmin() + "}"
        );
        log.info("Master admin flag userId={} value={} by={}", userId, request.masterAdmin(), adminId);
    }

    @Override
    @Transactional
    public void softDelete(UUID adminId, UUID userId) {
        requireMasterAdmin(adminId);
        if (adminId.equals(userId)) {
            throw new ForbiddenException("You cannot delete your own account");
        }
        User user = requireUser(userId);
        if (user.getDeletedAt() != null) {
            throw new ConflictException("User is already deleted");
        }
        user.setDeletedAt(Instant.now());
        user.setActive(false);
        userRepository.save(user);
        authSessionRevoker.revoke(userId);
        auditWriter.write(adminId, "USER_DELETED", "USER", userId, null);
        log.info("Admin soft-deleted userId={} by={}", userId, adminId);
    }

    @Override
    @Transactional
    public void restore(UUID adminId, UUID userId) {
        requireMasterAdmin(adminId);
        User user = requireUser(userId);
        if (user.getDeletedAt() == null) {
            throw new ConflictException("User is not deleted");
        }
        user.setDeletedAt(null);
        user.setActive(true);
        userRepository.save(user);
        auditWriter.write(adminId, "USER_RESTORED", "USER", userId, null);
        log.info("Admin restored userId={} by={}", userId, adminId);
    }

    private void requireMasterAdmin(UUID adminId) {
        User admin = requireUser(adminId);
        if (!admin.isMasterAdmin() || !admin.isActive() || admin.getDeletedAt() != null) {
            throw new ForbiddenException("Master admin required");
        }
    }

    private User requireUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private static UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getAvatarUrl(),
                user.isEmailVerified(),
                user.isActive(),
                user.isMasterAdmin()
        );
    }

    private static AdminUserResponse toAdmin(User user) {
        return new AdminUserResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getAvatarUrl(),
                user.isEmailVerified(),
                user.isActive(),
                user.isMasterAdmin(),
                user.getCreatedAt(),
                user.getLastLoginAt(),
                user.getDeletedAt()
        );
    }
}
