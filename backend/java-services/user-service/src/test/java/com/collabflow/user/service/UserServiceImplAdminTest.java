package com.collabflow.user.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.collabflow.common.exception.ForbiddenException;
import com.collabflow.user.dto.request.SetMasterAdminRequest;
import com.collabflow.user.model.User;
import com.collabflow.user.repository.UserRepository;
import com.collabflow.user.service.impl.UserServiceImpl;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceImplAdminTest {

    @Mock UserRepository userRepository;
    @Mock ApplicationEventPublisher eventPublisher;
    @Mock AdminUserDirectory adminUserDirectory;
    @Mock AdminAuditWriter auditWriter;
    @Mock AuthSessionRevoker authSessionRevoker;

    UserServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new UserServiceImpl(
                userRepository,
                new BCryptPasswordEncoder(10),
                eventPublisher,
                adminUserDirectory,
                auditWriter,
                authSessionRevoker
        );
    }

    @Test
    void cannotStripOwnMasterAdminOrDeleteSelf() {
        UUID adminId = UUID.randomUUID();
        User admin = new User();
        admin.setId(adminId);
        admin.setMasterAdmin(true);
        admin.setActive(true);
        when(userRepository.findById(adminId)).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> service.setMasterAdmin(adminId, adminId, new SetMasterAdminRequest(false)))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("own master admin");

        assertThatThrownBy(() -> service.softDelete(adminId, adminId))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("own account");
    }
}
