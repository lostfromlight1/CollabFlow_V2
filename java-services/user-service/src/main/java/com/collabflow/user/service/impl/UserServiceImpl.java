package com.collabflow.user.service.impl;

import com.collabflow.common.exception.BusinessException;
import com.collabflow.common.exception.ConflictException;
import com.collabflow.common.exception.ErrorCode;
import com.collabflow.common.exception.ResourceNotFoundException;
import com.collabflow.user.dto.request.AdminCreateUserRequest;
import com.collabflow.user.dto.request.UpdateProfileRequest;
import com.collabflow.user.dto.response.UserResponse;
import com.collabflow.user.event.EmailSendDomainEvent;
import com.collabflow.user.event.EmailSendPayload;
import com.collabflow.user.event.UserCreatedDomainEvent;
import com.collabflow.user.event.UserCreatedPayload;
import com.collabflow.user.event.UserUpdatedDomainEvent;
import com.collabflow.user.event.UserUpdatedPayload;
import com.collabflow.user.model.User;
import com.collabflow.user.repository.UserRepository;
import com.collabflow.user.service.UserService;
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

    public UserServiceImpl(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            ApplicationEventPublisher eventPublisher
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.eventPublisher = eventPublisher;
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
        return toResponse(user);
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
}
