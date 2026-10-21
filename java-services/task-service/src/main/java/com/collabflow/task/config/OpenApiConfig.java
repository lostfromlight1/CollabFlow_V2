package com.collabflow.task.config;

import com.collabflow.common.openapi.OpenApiSupport;
import io.swagger.v3.oas.models.OpenAPI;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI taskOpenApi() {
        return OpenApiSupport.baseOpenApi(
                "CollabFlow Task Service",
                "Tasks and assignments. All /api/** require X-API-Key.",
                "v1"
        );
    }
}
