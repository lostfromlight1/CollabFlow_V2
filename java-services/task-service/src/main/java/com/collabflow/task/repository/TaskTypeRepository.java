package com.collabflow.task.repository;

import com.collabflow.task.model.TaskType;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskTypeRepository extends JpaRepository<TaskType, UUID> {

    List<TaskType> findBySystemTrueOrderByNameAsc();
}
