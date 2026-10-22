package com.collabflow.auth.repository;

import com.collabflow.auth.model.OrganizationRef;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationRefRepository extends JpaRepository<OrganizationRef, UUID> {

    List<OrganizationRef> findByIdInAndActiveTrue(Collection<UUID> ids);
}
