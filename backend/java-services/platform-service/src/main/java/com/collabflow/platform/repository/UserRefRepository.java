package com.collabflow.platform.repository;

import com.collabflow.platform.model.UserRef;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRefRepository extends JpaRepository<UserRef, UUID> {

    boolean existsByIdAndActiveTrue(UUID id);

    boolean existsByIdAndActiveTrueAndMasterAdminTrue(UUID id);
}
