package com.collabflow.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.collabflow.auth.config.AuthPhase2Properties;
import com.collabflow.auth.config.JwtProperties;
import com.collabflow.auth.config.JwtService;
import com.collabflow.auth.dto.request.RegisterRequest;
import com.collabflow.auth.event.EmailSendDomainEvent;
import com.collabflow.auth.event.UserCreatedDomainEvent;
import com.collabflow.auth.model.User;
import com.collabflow.auth.repository.EmailTokenRepository;
import com.collabflow.auth.repository.OrganizationMemberRefRepository;
import com.collabflow.auth.repository.OrganizationRefRepository;
import com.collabflow.auth.repository.RefreshTokenRepository;
import com.collabflow.auth.repository.UserRepository;
import com.collabflow.auth.service.impl.AuthServiceImpl;
import com.collabflow.common.exception.BusinessException;
import com.collabflow.common.exception.ConflictException;
import java.util.List;
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
                eventPublisher
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
}
