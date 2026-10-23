package com.collabflow.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.collabflow.auth.config.AuthPhase2Properties;
import com.collabflow.auth.config.JwtProperties;
import com.collabflow.auth.config.JwtService;
import com.collabflow.auth.dto.request.AdminResetPasswordRequest;
import com.collabflow.auth.dto.request.ImpersonateRequest;
import com.collabflow.auth.dto.request.LoginRequest;
import com.collabflow.auth.dto.request.RegisterRequest;
import com.collabflow.auth.event.EmailSendDomainEvent;
import com.collabflow.auth.event.UserCreatedDomainEvent;
import com.collabflow.auth.model.User;
import com.collabflow.auth.repository.EmailTokenRepository;
import com.collabflow.auth.repository.OrganizationMemberRefRepository;
import com.collabflow.auth.repository.OrganizationRefRepository;
import com.collabflow.auth.repository.RefreshTokenRepository;
import com.collabflow.auth.repository.UserRepository;
import com.collabflow.auth.service.AdminAuditWriter;
import com.collabflow.auth.service.impl.AuthServiceImpl;
import com.collabflow.common.exception.BusinessException;
import com.collabflow.common.exception.ConflictException;
import com.collabflow.common.exception.ForbiddenException;
import com.collabflow.common.exception.UnauthorizedException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    UserRepository userRepository;
    @Mock
    RefreshTokenRepository refreshTokenRepository;
    @Mock
    EmailTokenRepository emailTokenRepository;
    @Mock
    OrganizationMemberRefRepository organizationMemberRefRepository;
    @Mock
    OrganizationRefRepository organizationRefRepository;
    @Mock
    ApplicationEventPublisher eventPublisher;
    @Mock
    AdminAuditWriter auditWriter;

    JwtProperties jwtProperties;
    AuthPhase2Properties authProperties;
    JwtService jwtService;
    AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        jwtProperties = new JwtProperties();
        jwtProperties.setSecret("collabflow-dev-jwt-secret-change-me-32c");
        jwtProperties.setAccessTokenMinutes(30);
        jwtProperties.setRefreshTokenDays(7);
        jwtService = new JwtService(jwtProperties);

        authProperties = new AuthPhase2Properties();
        authProperties.setPublicRegisterEnabled(true);
        authProperties.setVerifyTokenHours(48);
        authProperties.setPublicAppBaseUrl("http://localhost:3000");

        authService = new AuthServiceImpl(
                userRepository,
                refreshTokenRepository,
                emailTokenRepository,
                organizationMemberRefRepository,
                organizationRefRepository,
                new BCryptPasswordEncoder(10),
                jwtService,
                jwtProperties,
                authProperties,
                eventPublisher,
                auditWriter
        );
    }

    @Test
    void register_persistsUserAndReturnsTokens() {
        when(userRepository.existsByEmailIgnoreCase("kaung@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(UUID.randomUUID());
            return u;
        });
        when(emailTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(organizationMemberRefRepository.findByUserId(any())).thenReturn(List.of());

        var response = authService.register(
                new RegisterRequest("kaung@example.com", "password123", "Kaung")
        );

        assertThat(response.email()).isEqualTo("kaung@example.com");
        assertThat(response.userId()).isNotNull();
        assertThat(response.accessToken()).isNotBlank();
        assertThat(response.refreshToken()).isNotBlank();

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getPassword()).startsWith("$2");
        verify(eventPublisher).publishEvent(any(UserCreatedDomainEvent.class));
        verify(eventPublisher).publishEvent(any(EmailSendDomainEvent.class));
        verify(emailTokenRepository).save(any());
    }

    @Test
    void register_whenPublicDisabled_throwsForbidden() {
        authProperties.setPublicRegisterEnabled(false);

        assertThatThrownBy(() -> authService.register(
                new RegisterRequest("kaung@example.com", "password123", "Kaung")
        )).isInstanceOf(BusinessException.class)
                .hasMessageContaining("Public registration is disabled");
    }

    @Test
    void register_DuplicateEmail_ThrowsConflict() {
        when(userRepository.existsByEmailIgnoreCase("kaung@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(
                new RegisterRequest("kaung@example.com", "password123", "Kaung")
        )).isInstanceOf(ConflictException.class);
    }

    @Test
    void adminDeactivate_rejectsSelfAndDeleted() {
        UUID adminId = UUID.randomUUID();
        User admin = master(adminId);
        when(userRepository.findById(adminId)).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> authService.adminDeactivate(adminId, adminId))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("own account");

        UUID targetId = UUID.randomUUID();
        User deleted = user(targetId);
        deleted.setDeletedAt(java.time.Instant.now());
        when(userRepository.findById(targetId)).thenReturn(Optional.of(deleted));
        assertThatThrownBy(() -> authService.adminDeactivate(adminId, targetId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Restore");
    }

    @Test
    void login_deletedUser_distinctMessage() {
        User user = user(UUID.randomUUID());
        user.setEmail("gone@example.com");
        user.setPassword(new BCryptPasswordEncoder(10).encode("password123"));
        user.setEmailVerified(true);
        user.setDeletedAt(java.time.Instant.now());
        when(userRepository.findByEmailIgnoreCase("gone@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(new LoginRequest("gone@example.com", "password123", null)))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("deleted");
    }

    @Test
    void login_deactivatedUser_distinctMessage() {
        User user = user(UUID.randomUUID());
        user.setEmail("off@example.com");
        user.setPassword(new BCryptPasswordEncoder(10).encode("password123"));
        user.setEmailVerified(true);
        user.setActive(false);
        when(userRepository.findByEmailIgnoreCase("off@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(new LoginRequest("off@example.com", "password123", null)))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("deactivated");
    }

    @Test
    void impersonate_rejectsSelfMasterAdminAndInactive() {
        UUID adminId = UUID.randomUUID();
        when(userRepository.findById(adminId)).thenReturn(Optional.of(master(adminId)));

        assertThatThrownBy(() -> authService.impersonate(adminId, new ImpersonateRequest(adminId)))
                .isInstanceOf(ForbiddenException.class);

        UUID otherAdmin = UUID.randomUUID();
        when(userRepository.findById(otherAdmin)).thenReturn(Optional.of(master(otherAdmin)));
        assertThatThrownBy(() -> authService.impersonate(adminId, new ImpersonateRequest(otherAdmin)))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("another master admin");

        UUID inactiveId = UUID.randomUUID();
        User inactive = user(inactiveId);
        inactive.setActive(false);
        when(userRepository.findById(inactiveId)).thenReturn(Optional.of(inactive));
        assertThatThrownBy(() -> authService.impersonate(adminId, new ImpersonateRequest(inactiveId)))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void adminResetPassword_revokesSessions() {
        UUID adminId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        when(userRepository.findById(adminId)).thenReturn(Optional.of(master(adminId)));
        when(userRepository.findById(targetId)).thenReturn(Optional.of(user(targetId)));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        authService.adminResetPassword(adminId, targetId, new AdminResetPasswordRequest("new-password"));

        verify(refreshTokenRepository).revokeAllActiveForUser(targetId);
        verify(auditWriter).write(adminId, "USER_PASSWORD_RESET", "USER", targetId, null);
    }

    @Test
    void adminResend_alreadyVerified_doesNotSend() {
        UUID adminId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        User target = user(targetId);
        target.setEmailVerified(true);
        when(userRepository.findById(adminId)).thenReturn(Optional.of(master(adminId)));
        when(userRepository.findById(targetId)).thenReturn(Optional.of(target));

        assertThat(authService.adminResendVerification(adminId, targetId)).isEqualTo("Already verified");
        verify(eventPublisher, org.mockito.Mockito.never()).publishEvent(any());
    }

    private static User master(UUID id) {
        User user = user(id);
        user.setMasterAdmin(true);
        user.setEmailVerified(true);
        return user;
    }

    private static User user(UUID id) {
        User user = new User();
        user.setId(id);
        user.setEmail(id + "@example.com");
        user.setName("Test");
        user.setPassword("hash");
        user.setActive(true);
        return user;
    }
}
