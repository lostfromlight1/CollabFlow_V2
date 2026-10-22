package com.collabflow.task.service;

import com.collabflow.common.exception.BusinessException;
import com.collabflow.common.exception.ErrorCode;
import com.collabflow.task.repository.ProjectRefRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class PermissionService {

    private final ProjectRefRepository projectRefRepository;

    public PermissionService(ProjectRefRepository projectRefRepository) {
        this.projectRefRepository = projectRefRepository;
    }

    public void requireProjectPermission(UUID projectId, UUID userId, String permission) {
        if (projectRefRepository.countWithPermission(projectId, userId, permission) == 0) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Missing permission: " + permission);
        }
    }

    public void requireProjectMember(UUID projectId, UUID userId) {
        if (projectRefRepository.countMembers(projectId, userId) == 0) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Not a member of this project");
        }
    }

    public boolean isProjectMember(UUID projectId, UUID userId) {
        return projectRefRepository.countMembers(projectId, userId) > 0;
    }
}
