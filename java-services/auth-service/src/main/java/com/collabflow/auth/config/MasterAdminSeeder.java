package com.collabflow.auth.config;

import com.collabflow.auth.model.User;
import com.collabflow.auth.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ensures a master admin exists for the admin portal (idempotent).
 * Password from env {@code MASTER_ADMIN_PASSWORD} / collabflow.auth.master-admin-password.
 */
@Component
public class MasterAdminSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(MasterAdminSeeder.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthPhase2Properties properties;

    public MasterAdminSeeder(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthPhase2Properties properties
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        String email = properties.getMasterAdminEmail().trim().toLowerCase();
        userRepository.findByEmailIgnoreCase(email).ifPresentOrElse(existing -> {
            if (!existing.isMasterAdmin() || !existing.isEmailVerified() || !existing.isActive()) {
                existing.setMasterAdmin(true);
                existing.setEmailVerified(true);
                existing.setActive(true);
                userRepository.save(existing);
                log.info("Promoted existing user to master admin email={}", email);
            }
        }, () -> {
            User admin = new User();
            admin.setEmail(email);
            admin.setName(properties.getMasterAdminName());
            admin.setPassword(passwordEncoder.encode(properties.getMasterAdminPassword()));
            admin.setMasterAdmin(true);
            admin.setEmailVerified(true);
            admin.setActive(true);
            userRepository.save(admin);
            log.info("Seeded master admin id={} email={}", admin.getId(), email);
        });
    }
}
