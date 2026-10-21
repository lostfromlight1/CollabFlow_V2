package com.collabflow.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.collabflow.auth.config.JwtProperties;
import com.collabflow.auth.config.JwtService;
import com.collabflow.auth.dto.request.RegisterRequest;
import com.collabflow.auth.model.User;
import com.collabflow.auth.repository.RefreshTokenRepository;
import com.collabflow.auth.repository.UserRepository;
import com.collabflow.auth.event.UserCreatedDomainEvent;
import com.collabflow.auth.service.impl.AuthServiceImpl;
import com.collabflow.common.exception.ConflictException;
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
    ApplicationEventPublisher eventPublisher;

    JwtProperties jwtProperties;
    JwtService jwtService;
    AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        jwtProperties = new JwtProperties();
        jwtProperties.setSecret("collabflow-dev-jwt-secret-change-me-32c");
        jwtProperties.setAccessTokenMinutes(30);
        jwtProperties.setRefreshTokenDays(7);
        jwtService = new JwtService(jwtProperties);
        authService = new AuthServiceImpl(
                userRepository,
                refreshTokenRepository,
                new BCryptPasswordEncoder(10),
                jwtService,
                jwtProperties,
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
        when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var response = authService.register(
                new RegisterRequest("kaung@example.com", "password123", "Kaung")
        );

        assertThat(response.email()).isEqualTo("kaung@example.com");
        assertThat(response.accessToken()).isNotBlank();
        assertThat(response.refreshToken()).isNotBlank();

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getPassword()).startsWith("$2");
        verify(eventPublisher).publishEvent(any(UserCreatedDomainEvent.class));
    }

    @Test
    void register_DuplicateEmail_ThrowsConflict() {
        when(userRepository.existsByEmailIgnoreCase("kaung@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(
                new RegisterRequest("kaung@example.com", "password123", "Kaung")
        )).isInstanceOf(ConflictException.class);
    }
}
