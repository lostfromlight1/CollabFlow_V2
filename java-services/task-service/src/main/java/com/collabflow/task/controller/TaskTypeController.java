package com.collabflow.task.controller;

import com.collabflow.common.api.ApiResponse;
import com.collabflow.common.openapi.OpenApiSupport;
import com.collabflow.common.web.ApiId;
import com.collabflow.task.dto.response.TaskTypeResponse;
import com.collabflow.task.service.WorkflowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/task-types")
@Tag(name = "Task types")
@SecurityRequirement(name = OpenApiSupport.API_KEY_SCHEME)
@SecurityRequirement(name = OpenApiSupport.BEARER_SCHEME)
public class TaskTypeController {

    private static final Logger log = LoggerFactory.getLogger(TaskTypeController.class);

    private final WorkflowService workflowService;

    public TaskTypeController(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    @GetMapping
    @ApiId("WF-004")
    @Operation(summary = "List system task types")
    public ApiResponse<List<TaskTypeResponse>> list() {
        log.info("GET /task-types");
        return ApiResponse.ok(workflowService.listTaskTypes());
    }
}
