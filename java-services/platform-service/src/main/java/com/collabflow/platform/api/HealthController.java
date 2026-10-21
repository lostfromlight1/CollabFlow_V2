package com.collabflow.platform.api;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Stub — org/workspace/project APIs in Week 5–7. */
@RestController
@RequestMapping("/api/v1/platform")
public class HealthController {

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of(
                "service", "platform-service",
                "status", "UP"
        );
    }
}
