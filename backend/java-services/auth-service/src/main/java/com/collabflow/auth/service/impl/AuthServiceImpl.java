package com.collabflow.auth.service.impl;

import com.collabflow.auth.config.AuthPhase2Properties;
import com.collabflow.auth.config.JwtProperties;
import com.collabflow.auth.config.JwtService;
import com.collabflow.auth.dto.request.AdminResetPasswordRequest;
import com.collabflow.auth.dto.request.ChangePasswordRequest;
import com.collabflow.auth.dto.request.ForgotPasswordRequest;
import com.collabflow.auth.dto.request.ImpersonateRequest;
import com.collabflow.auth.dto.request.LoginRequest;
import com.collabflow.auth.dto.request.RefreshRequest;
import com.collabflow.auth.dto.request.RegisterRequest;
import com.collabflow.auth.dto.request.ResetPasswordRequest;
import com.collabflow.auth.dto.request.ResendVerificationRequest;
import com.collabflow.auth.dto.request.SelectOrganizationRequest;
import com.collabflow.auth.dto.request.UpdateProfileRequest;
import com.collabflow.auth.dto.request.VerifyEmailRequest;
import com.collabflow.auth.dto.response.AuthResponse;
import com.collabflow.auth.dto.response.ImpersonationResponse;
import com.collabflow.auth.dto.response.OrganizationSummary;
import com.collabflow.auth.dto.response.SessionResponse;
import com.collabflow.auth.dto.response.UserResponse;
import com.collabflow.auth.event.EmailSendDomainEvent;
import com.collabflow.auth.event.EmailSendPayload;
import com.collabflow.auth.event.UserCreatedDomainEvent;
import com.collabflow.auth.event.UserCreatedPayload;
import com.collabflow.auth.model.EmailToken;
import com.collabflow.auth.model.OrganizationMemberRef;
import com.collabflow.auth.model.OrganizationRef;
import com.collabflow.auth.model.RefreshToken;
import com.collabflow.auth.model.User;
import com.collabflow.auth.repository.EmailTokenRepository;
import com.collabflow.auth.repository.OrganizationMemberRefRepository;
import com.collabflow.auth.repository.OrganizationRefRepository;
import com.collabflow.auth.repository.RefreshTokenRepository;
import com.collabflow.auth.repository.UserRepository;
import com.collabflow.auth.service.AdminAuditWriter;
import com.collabflow.auth.service.AuthService;
import com.collabflow.common.exception.BusinessException;
import com.collabflow.common.exception.ConflictException;
import com.collabflow.common.exception.ErrorCode;
import com.collabflow.common.exception.ForbiddenException;
import com.collabflow.common.exception.ResourceNotFoundException;
import com.collabflow.common.exception.UnauthorizedException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final EmailTokenRepository emailTokenRepository;
    private final OrganizationMemberRefRepository organizationMemberRefRepository;
    private final OrganizationRefRepository organizationRefRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final AuthPhase2Properties authProperties;
    private final ApplicationEventPublisher eventPublisher;
    private final AdminAuditWriter auditWriter;

    public AuthServiceImpl(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            EmailTokenRepository emailTokenRepository,
            OrganizationMemberRefRepository organizationMemberRefRepository,
            OrganizationRefRepository organizationRefRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            JwtProperties jwtProperties,
            AuthPhase2Properties authProperties,
            ApplicationEventPublisher eventPublisher,
            AdminAuditWriter auditWriter
    ) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.emailTokenRepository = emailTokenRepository;
        this.organizationMemberRefRepository = organizationMemberRefRepository;
        this.organizationRefRepository = organizationRefRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.jwtProperties = jwtProperties;
        this.authProperties = authProperties;
        this.eventPublisher = eventPublisher;
        this.auditWriter = auditWriter;
    }

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (!authProperties.isPublicRegisterEnabled()) {
            throw new BusinessException(
                    ErrorCode.FORBIDDEN,
                    "Public registration is disabled; ask an admin to create your account"
            );
        }
        User user = createUser(
                request.email(),
                request.password(),
                request.name(),
                false
        );
        issueVerifyEmail(user);
        // Register may complete before org membership — do not require orgs here (login does).
        return issueTokensWithOrgContext(user, null, false);
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase();
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            log.warn("Login failed for email={}", email);
            throw new UnauthorizedException("Invalid email or password");
        }
        if (user.getDeletedAt() != null) {
            throw new UnauthorizedException("This account has been deleted");
        }
        if (!user.isActive()) {
            throw new UnauthorizedException("This account has been deactivated");
        }

        if (!user.isEmailVerified() && !user.isMasterAdmin()) {
            throw new BusinessException(
                    ErrorCode.FORBIDDEN,
                    "Email not verified. Check your inbox or resend verification."
            );
        }

        user.setLastLoginAt(Instant.now());
        userRepository.save(user);
        log.info("Login ok userId={}", user.getId());
        return issueTokensWithOrgContext(user, request.organizationId(), true);
    }

    @Override
    @Transactional
    public AuthResponse refresh(RefreshRequest request) {
        RefreshToken stored = refreshTokenRepository.findByTokenAndRevokedFalse(request.refreshToken())
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        if (stored.getExpiresAt().isBefore(Instant.now())) {
            throw new UnauthorizedException("Refresh token expired");
        }

        stored.setRevoked(true);
        stored.setRevokedAt(Instant.now());
        refreshTokenRepository.save(stored);

        User user = stored.getUser();
        if (user.getDeletedAt() != null) {
            throw new UnauthorizedException("This account has been deleted");
        }
        if (!user.isActive()) {
            throw new UnauthorizedException("This account has been deactivated");
        }

        // Refresh keeps session even if orgs were removed; product APIs still require membership.
        return issueTokensWithOrgContext(user, null, false);
    }

    @Override
    @Transactional
    public AuthResponse selectOrganization(UUID userId, SelectOrganizationRequest request) {
        User user = requireUser(userId);
        return issueTokensWithOrgContext(user, request.organizationId(), true);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrganizationSummary> listMyOrganizations(UUID userId) {
        requireUser(userId);
        return loadOrganizationSummaries(userId);
    }

    @Override
    @Transactional
    public void logout(String refreshToken) {
        refreshTokenRepository.findByTokenAndRevokedFalse(refreshToken).ifPresent(token -> {
            token.setRevoked(true);
            token.setRevokedAt(Instant.now());
            refreshTokenRepository.save(token);
        });
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
        return toResponse(userRepository.save(user));
    }

    @Override
    @Transactional
    public void verifyEmail(VerifyEmailRequest request) {
        EmailToken token = emailTokenRepository
                .findByTokenAndPurposeAndUsedAtIsNull(request.token(), EmailToken.PURPOSE_VERIFY)
                .orElseThrow(() -> new BusinessException(ErrorCode.BAD_REQUEST, "Invalid or used verification token"));
        if (token.getExpiresAt().isBefore(Instant.now())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Verification token expired");
        }
        User user = token.getUser();
        user.setEmailVerified(true);
        userRepository.save(user);
        token.setUsedAt(Instant.now());
        emailTokenRepository.save(token);
        log.info("Email verified userId={}", user.getId());
    }

    @Override
    @Transactional
    public void resendVerification(ResendVerificationRequest request) {
        userRepository.findByEmailIgnoreCase(request.email().trim().toLowerCase()).ifPresent(user -> {
            if (!user.isEmailVerified()) {
                issueVerifyEmail(user);
            }
        });
        // Always succeed to avoid email enumeration
    }

    @Override
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        userRepository.findByEmailIgnoreCase(request.email().trim().toLowerCase()).ifPresent(user -> {
            String raw = newToken();
            EmailToken token = new EmailToken();
            token.setUser(user);
            token.setToken(raw);
            token.setPurpose(EmailToken.PURPOSE_RESET);
            token.setExpiresAt(Instant.now().plus(authProperties.getResetTokenHours(), ChronoUnit.HOURS));
            emailTokenRepository.save(token);

            String link = authProperties.getPublicAppBaseUrl() + "/reset-password?token=" + raw;
            eventPublisher.publishEvent(new EmailSendDomainEvent(EmailSendPayload.of(
                    user.getEmail(),
                    "Reset your CollabFlow password",
                    "Reset link (valid " + authProperties.getResetTokenHours() + "h): " + link,
                    "password_reset"
            )));
        });
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        EmailToken token = emailTokenRepository
                .findByTokenAndPurposeAndUsedAtIsNull(request.token(), EmailToken.PURPOSE_RESET)
                .orElseThrow(() -> new BusinessException(ErrorCode.BAD_REQUEST, "Invalid or used reset token"));
        if (token.getExpiresAt().isBefore(Instant.now())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Reset token expired");
        }
        User user = token.getUser();
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
        token.setUsedAt(Instant.now());
        emailTokenRepository.save(token);
        refreshTokenRepository.revokeAllActiveForUser(user.getId());
        log.info("Password reset userId={}", user.getId());
    }

    @Override
    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequest request) {
        User user = requireUser(userId);
        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new UnauthorizedException("Current password is incorrect");
        }
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
        refreshTokenRepository.revokeAllActiveForUser(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SessionResponse> listSessions(UUID userId) {
        requireUser(userId);
        return refreshTokenRepository.findByUser_IdAndRevokedFalseOrderByCreatedAtDesc(userId).stream()
                .map(t -> new SessionResponse(t.getId(), t.getCreatedAt(), t.getExpiresAt()))
                .toList();
    }

    @Override
    @Transactional
    public void revokeAllSessions(UUID userId) {
        requireUser(userId);
        refreshTokenRepository.revokeAllActiveForUser(userId);
    }

    @Override
    @Transactional
    public void deactivate(UUID userId) {
        User user = requireUser(userId);
        user.setActive(false);
        userRepository.save(user);
        refreshTokenRepository.revokeAllActiveForUser(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isMasterAdmin(UUID userId) {
        return userRepository.findById(userId).map(User::isMasterAdmin).orElse(false);
    }

    @Override
    @Transactional
    public void adminDeactivate(UUID adminId, UUID userId) {
        requireActiveMaster(adminId);
        if (adminId.equals(userId)) {
            throw new ForbiddenException("You cannot deactivate your own account");
        }
        User user = requireUser(userId);
        if (user.getDeletedAt() != null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Restore the user before changing status");
        }
        user.setActive(false);
        userRepository.save(user);
        refreshTokenRepository.revokeAllActiveForUser(userId);
        auditWriter.write(adminId, "USER_DEACTIVATED", "USER", userId, null);
        log.info("Admin deactivated userId={} by={}", userId, adminId);
    }

    @Override
    @Transactional
    public void adminActivate(UUID adminId, UUID userId) {
        requireActiveMaster(adminId);
        User user = requireUser(userId);
        if (user.getDeletedAt() != null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Restore the user before activating");
        }
        user.setActive(true);
        userRepository.save(user);
        auditWriter.write(adminId, "USER_ACTIVATED", "USER", userId, null);
        log.info("Admin activated userId={} by={}", userId, adminId);
    }

    @Override
    @Transactional
    public void adminRevokeSessions(UUID adminId, UUID userId) {
        requireActiveMaster(adminId);
        requireUser(userId);
        refreshTokenRepository.revokeAllActiveForUser(userId);
        auditWriter.write(adminId, "USER_SESSIONS_REVOKED", "USER", userId, null);
        log.info("Admin revoked sessions userId={} by={}", userId, adminId);
    }

    @Override
    @Transactional
    public void adminResetPassword(UUID adminId, UUID userId, AdminResetPasswordRequest request) {
        requireActiveMaster(adminId);
        User user = requireUser(userId);
        user.setPassword(passwordEncoder.encode(request.temporaryPassword()));
        userRepository.save(user);
        refreshTokenRepository.revokeAllActiveForUser(userId);
        auditWriter.write(adminId, "USER_PASSWORD_RESET", "USER", userId, null);
        log.info("Admin reset password userId={} by={}", userId, adminId);
    }

    @Override
    @Transactional
    public String adminResendVerification(UUID adminId, UUID userId) {
        requireActiveMaster(adminId);
        User user = requireUser(userId);
        if (user.isEmailVerified()) {
            return "Already verified";
        }
        issueVerifyEmail(user);
        auditWriter.write(adminId, "USER_VERIFICATION_RESENT", "USER", userId, null);
        log.info("Admin resent verification userId={} by={}", userId, adminId);
        return "Verification email sent";
    }

    @Override
    @Transactional
    public ImpersonationResponse impersonate(UUID adminId, ImpersonateRequest request) {
        requireActiveMaster(adminId);
        if (adminId.equals(request.userId())) {
            throw new ForbiddenException("Cannot impersonate yourself");
        }
        User target = requireUser(request.userId());
        if (target.isMasterAdmin()) {
            throw new ForbiddenException("Cannot impersonate another master admin");
        }
        if (target.getDeletedAt() != null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "User is deleted");
        }
        if (!target.isActive()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "User is deactivated");
        }
        Instant expiresAt = Instant.now().plus(15, ChronoUnit.MINUTES);
        String token = jwtService.createImpersonationToken(target.getId(), target.getEmail(), adminId);
        auditWriter.write(adminId, "IMPERSONATION_STARTED", "USER", target.getId(), null);
        log.info("Impersonation started admin={} target={}", adminId, target.getId());
        return new ImpersonationResponse(token, expiresAt, target.getId(), target.getEmail(), target.getName());
    }

    @Override
    @Transactional
    public void endImpersonation(UUID actorId, UUID targetId) {
        requireActiveMaster(actorId);
        auditWriter.write(actorId, "IMPERSONATION_ENDED", "USER", targetId, null);
        log.info("Impersonation ended actor={} target={}", actorId, targetId);
    }

    private User requireActiveMaster(UUID adminId) {
        User admin = requireUser(adminId);
        if (!admin.isMasterAdmin() || !admin.isActive() || admin.getDeletedAt() != null) {
            throw new ForbiddenException("Master admin required");
        }
        return admin;
    }

    private User createUser(String emailRaw, String rawPassword, String name, boolean masterAdmin) {
        String email = emailRaw.trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("Email already registered");
        }
        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setName(name.trim());
        user.setMasterAdmin(masterAdmin);
        if (masterAdmin) {
            user.setEmailVerified(true);
        }
        userRepository.save(user);
        log.info("Created user id={} email={} masterAdmin={}", user.getId(), email, masterAdmin);

        eventPublisher.publishEvent(new UserCreatedDomainEvent(
                new UserCreatedPayload(user.getId(), user.getEmail(), user.getName())
        ));
        return user;
    }

    private void issueVerifyEmail(User user) {
        String raw = newToken();
        EmailToken token = new EmailToken();
        token.setUser(user);
        token.setToken(raw);
        token.setPurpose(EmailToken.PURPOSE_VERIFY);
        token.setExpiresAt(Instant.now().plus(authProperties.getVerifyTokenHours(), ChronoUnit.HOURS));
        emailTokenRepository.save(token);

        String link = authProperties.getPublicAppBaseUrl() + "/verify-email?token=" + raw;
        eventPublisher.publishEvent(new EmailSendDomainEvent(EmailSendPayload.of(
                user.getEmail(),
                "Verify your CollabFlow email",
                "Verify link (valid " + authProperties.getVerifyTokenHours() + "h): " + link,
                "verify_email"
        )));
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

    private AuthResponse issueTokensWithOrgContext(User user, UUID requestedOrgId, boolean requireOrgMembership) {
        List<OrganizationSummary> orgs = loadOrganizationSummaries(user.getId());

        if (requireOrgMembership && orgs.isEmpty() && !user.isMasterAdmin()) {
            throw new BusinessException(
                    ErrorCode.FORBIDDEN,
                    "No organization membership. Ask an admin to add you to an organization."
            );
        }

        UUID selected = null;
        boolean requiresSelection = false;

        if (requestedOrgId != null) {
            boolean member = orgs.stream().anyMatch(o -> o.id().equals(requestedOrgId));
            if (!member && !user.isMasterAdmin()) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "Not a member of the selected organization");
            }
            selected = requestedOrgId;
        } else if (orgs.size() == 1) {
            selected = orgs.get(0).id();
        } else if (orgs.size() > 1) {
            requiresSelection = true;
        }

        String access = jwtService.createAccessToken(user.getId(), user.getEmail());
        String refresh = UUID.randomUUID() + "." + UUID.randomUUID();

        RefreshToken entity = new RefreshToken();
        entity.setUser(user);
        entity.setToken(refresh);
        entity.setExpiresAt(Instant.now().plus(jwtProperties.getRefreshTokenDays(), ChronoUnit.DAYS));
        refreshTokenRepository.save(entity);

        return AuthResponse.of(
                user.getId(),
                user.getEmail(),
                user.getName(),
                access,
                refresh,
                selected,
                orgs,
                requiresSelection
        );
    }

    private List<OrganizationSummary> loadOrganizationSummaries(UUID userId) {
        List<OrganizationMemberRef> memberships = organizationMemberRefRepository.findByUserId(userId);
        if (memberships.isEmpty()) {
            return List.of();
        }
        List<UUID> orgIds = memberships.stream().map(OrganizationMemberRef::getOrganizationId).toList();
        List<OrganizationRef> orgs = organizationRefRepository.findByIdInAndActiveTrue(orgIds);
        return orgs.stream()
                .map(org -> {
                    UUID roleId = memberships.stream()
                            .filter(m -> m.getOrganizationId().equals(org.getId()))
                            .map(OrganizationMemberRef::getRoleId)
                            .findFirst()
                            .orElse(null);
                    return new OrganizationSummary(org.getId(), org.getName(), org.getSlug(), roleId);
                })
                .toList();
    }

    private static String newToken() {
        return UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
    }
}
