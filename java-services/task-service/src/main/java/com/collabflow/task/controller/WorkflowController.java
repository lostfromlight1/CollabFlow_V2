package com.collabflow.task.controller;

import com.collabflow.common.api.ApiResponse;
import com.collabflow.common.openapi.OpenApiSupport;
import com.collabflow.common.web.ApiId;
import com.collabflow.task.dto.response.WorkflowResponse;
import com.collabflow.task.dto.response.WorkflowStatusResponse;
import com.collabflow.task.service.WorkflowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/workflows")
@Tag(name = "Workflows")
@SecurityRequirement(name = OpenApiSupport.API_KEY_SCHEME)
@SecurityRequirement(name = OpenApiSupport.BEARER_SCHEME)
public class WorkflowController {

    private static final Logger log = LoggerFactory.getLogger(WorkflowController.class);

    private final WorkflowService workflowService;

    public WorkflowController(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    @GetMapping("/default")
    @ApiId("WF-001")
    @Operation(summary = "Get default global workflow with statuses")
    public ApiResponse<WorkflowResponse> getDefault() {
        log.info("GET /workflows/default");
        return ApiResponse.ok(workflowService.getDefault());
    }

    @GetMapping("/{id}")
    @ApiId("WF-002")
    @Operation(summary = "Get workflow with statuses")
    public ApiResponse<WorkflowResponse> get(@PathVariable UUID id) {
        log.info("GET /workflows/{}", id);
        return ApiResponse.ok(workflowService.get(id));
    }

    @GetMapping("/{id}/statuses")
    @ApiId("WF-003")
    @Operation(summary = "List workflow statuses")
    public ApiResponse<List<WorkflowStatusResponse>> listStatuses(@PathVariable UUID id) {
        log.info("GET /workflows/{}/statuses", id);
        return ApiResponse.ok(workflowService.listStatuses(id));
    }
}
