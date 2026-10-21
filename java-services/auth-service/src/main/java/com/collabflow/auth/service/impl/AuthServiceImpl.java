package com.collabflow.auth.service.impl;

import com.collabflow.auth.config.JwtProperties;
import com.collabflow.auth.config.JwtService;
import com.collabflow.auth.dto.request.LoginRequest;
import com.collabflow.auth.dto.request.RefreshRequest;
import com.collabflow.auth.dto.request.RegisterRequest;
import com.collabflow.auth.dto.response.AuthResponse;
import com.collabflow.auth.dto.response.UserResponse;
import com.collabflow.auth.event.UserCreatedDomainEvent;
import com.collabflow.auth.event.UserCreatedPayload;
import com.collabflow.auth.model.RefreshToken;
import com.collabflow.auth.model.User;
import com.collabflow.auth.repository.RefreshTokenRepository;
import com.collabflow.auth.repository.UserRepository;
import com.collabflow.auth.service.AuthService;
import com.collabflow.common.exception.ConflictException;
import com.collabflow.common.exception.ResourceNotFoundException;
import com.collabflow.common.exception.UnauthorizedException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final ApplicationEventPublisher eventPublisher;

    public AuthServiceImpl(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            JwtProperties jwtProperties,
            ApplicationEventPublisher eventPublisher
    ) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.jwtProperties = jwtProperties;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("Email already registered");
        }

        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setName(request.name().trim());
        userRepository.save(user);
        log.info("Registered user id={} email={}", user.getId(), email);

        // Fired after commit → Kafka (see UserCreatedKafkaPublisher)
        eventPublisher.publishEvent(new UserCreatedDomainEvent(
                new UserCreatedPayload(user.getId(), user.getEmail(), user.getName())
        ));

        return issueTokens(user);
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        // TODO (later): resolve organization first, then authenticate membership + credentials
        String email = request.email().trim().toLowerCase();
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        if (!user.isActive() || !passwordEncoder.matches(request.password(), user.getPassword())) {
            log.warn("Login failed for email={}", email);
            throw new UnauthorizedException("Invalid email or password");
        }

        log.info("Login ok userId={}", user.getId());
        return issueTokens(user);
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

        return issueTokens(stored.getUser());
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
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.isEmailVerified(),
                user.isActive()
        );
    }

    private AuthResponse issueTokens(User user) {
        String access = jwtService.createAccessToken(user.getId(), user.getEmail());
        String refresh = UUID.randomUUID() + "." + UUID.randomUUID();

        RefreshToken entity = new RefreshToken();
        entity.setUser(user);
        entity.setToken(refresh);
        entity.setExpiresAt(Instant.now().plus(jwtProperties.getRefreshTokenDays(), ChronoUnit.DAYS));
        refreshTokenRepository.save(entity);

        return AuthResponse.of(user.getId(), user.getEmail(), user.getName(), access, refresh);
    }
}
