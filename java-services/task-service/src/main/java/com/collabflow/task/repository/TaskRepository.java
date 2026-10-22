package com.collabflow.task.repository;

import com.collabflow.task.model.Task;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TaskRepository extends JpaRepository<Task, UUID> {

    List<Task> findByProjectIdOrderByCreatedAtDesc(UUID projectId);

    List<Task> findByParentTaskIdOrderByCreatedAtAsc(UUID parentTaskId);

    long countByProjectId(UUID projectId);

    @Query("""
            select t from Task t
            where t.projectId = :projectId
              and (:statusId is null or t.statusId = :statusId)
              and (:assigneeId is null or t.assigneeId = :assigneeId)
              and (:priority is null or t.priority = :priority)
              and (:sprintId is null or t.sprintId = :sprintId)
            """)
    Page<Task> findFiltered(
            @Param("projectId") UUID projectId,
            @Param("statusId") UUID statusId,
            @Param("assigneeId") UUID assigneeId,
            @Param("priority") String priority,
            @Param("sprintId") UUID sprintId,
            Pageable pageable
    );

    @Query("select coalesce(sum(t.storyPoints), 0) from Task t where t.sprintId = :sprintId")
    Long sumStoryPointsBySprintId(@Param("sprintId") UUID sprintId);

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
