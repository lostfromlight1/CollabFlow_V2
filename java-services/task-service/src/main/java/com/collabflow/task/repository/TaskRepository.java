package com.collabflow.task.repository;

import com.collabflow.task.model.Task;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TaskRepository extends JpaRepository<Task, UUID> {

    List<Task> findByProjectIdOrderByCreatedAtDesc(UUID projectId);

    long countByProjectId(UUID projectId);

    @Query("""
            select t.statusId, ws.name, ws.category, count(t)
            from Task t
            join WorkflowStatus ws on ws.id = t.statusId
            where t.projectId = :projectId
            group by t.statusId, ws.name, ws.category, ws.displayOrder
            order by ws.displayOrder
            """)
    List<Object[]> countByStatusForProject(@Param("projectId") UUID projectId);

    @Query("""
            select t.priority, count(t)
            from Task t
            where t.projectId = :projectId
            group by t.priority
            order by t.priority
            """)
    List<Object[]> countByPriorityForProject(@Param("projectId") UUID projectId);
}
