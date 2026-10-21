package com.collabflow.task.api;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Stub — tasks/workflows in Week 8. */
@RestController
@RequestMapping("/api/v1/tasks")
public class HealthController {

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of(
                "service", "task-service",
                "status", "UP"
        );
    }
}
