package com.collabflow.platform.repository;

import com.collabflow.platform.model.Role;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, UUID> {

    List<Role> findByScopeOrderByNameAsc(String scope);

    Optional<Role> findByIdAndScope(UUID id, String scope);
}
