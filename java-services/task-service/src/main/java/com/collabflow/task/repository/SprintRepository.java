package com.collabflow.task.repository;

import com.collabflow.task.model.Sprint;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SprintRepository extends JpaRepository<Sprint, UUID> {

    List<Sprint> findByProjectIdOrderBySprintNumberDesc(UUID projectId);

    Optional<Sprint> findByIdAndProjectId(UUID id, UUID projectId);

    boolean existsByProjectIdAndStatus(UUID projectId, String status);

    @Query("select coalesce(max(s.sprintNumber), 0) from Sprint s where s.projectId = :projectId")
    int findMaxSprintNumber(@Param("projectId") UUID projectId);
}
