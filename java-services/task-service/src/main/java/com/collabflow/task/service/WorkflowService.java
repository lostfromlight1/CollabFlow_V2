package com.collabflow.task.service;

import com.collabflow.task.dto.response.TaskTypeResponse;
import com.collabflow.task.dto.response.WorkflowResponse;
import com.collabflow.task.dto.response.WorkflowStatusResponse;
import java.util.List;
import java.util.UUID;

public interface WorkflowService {

    WorkflowResponse getDefault();

    WorkflowResponse get(UUID workflowId);

    List<WorkflowStatusResponse> listStatuses(UUID workflowId);

    List<TaskTypeResponse> listTaskTypes();
}
