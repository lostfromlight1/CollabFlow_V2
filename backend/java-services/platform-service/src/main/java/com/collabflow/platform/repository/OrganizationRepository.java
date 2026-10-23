package com.collabflow.platform.repository;

import com.collabflow.platform.model.Organization;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrganizationRepository extends JpaRepository<Organization, UUID> {

    boolean existsBySlugIgnoreCase(String slug);

    Optional<Organization> findByIdAndActiveTrue(UUID id);

    @Query("""
            select o from Organization o
            where o.active = true
              and o.id in (
                select m.organizationId from OrganizationMember m where m.userId = :userId
              )
            order by o.name
            """)
    List<Organization> findActiveForUser(@Param("userId") UUID userId);
}
