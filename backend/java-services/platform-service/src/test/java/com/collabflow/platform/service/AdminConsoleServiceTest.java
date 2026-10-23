package com.collabflow.platform.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.collabflow.common.exception.ForbiddenException;
import com.collabflow.platform.repository.AnnouncementRepository;
import com.collabflow.platform.repository.FeatureFlagRepository;
import com.collabflow.platform.repository.OrganizationMemberRepository;
import com.collabflow.platform.repository.OrganizationRepository;
import com.collabflow.platform.repository.UserRefRepository;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

@ExtendWith(MockitoExtension.class)
class AdminConsoleServiceTest {

    @Mock NamedParameterJdbcTemplate jdbc;
    @Mock OrganizationRepository organizationRepository;
    @Mock OrganizationMemberRepository memberRepository;
    @Mock UserRefRepository userRefRepository;
    @Mock UserDisplayLookup userDisplayLookup;
    @Mock AnnouncementRepository announcementRepository;
    @Mock FeatureFlagRepository featureFlagRepository;
    @Mock AdminAuditWriter auditWriter;

    AdminConsoleService service;

    @BeforeEach
    void setUp() {
        service = new AdminConsoleService(
                jdbc,
                organizationRepository,
                memberRepository,
                userRefRepository,
                userDisplayLookup,
                announcementRepository,
                featureFlagRepository,
                auditWriter
        );
    }

    @Test
    void deactivateRequiresMasterAdmin() {
        UUID adminId = UUID.randomUUID();
        when(userRefRepository.existsByIdAndActiveTrueAndMasterAdminTrue(adminId)).thenReturn(false);
        assertThatThrownBy(() -> service.deactivateOrganization(adminId, UUID.randomUUID()))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Master admin");
    }
}
