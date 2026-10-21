package com.collabflow.platform.config;

import com.collabflow.common.openapi.OpenApiSupport;
import io.swagger.v3.oas.models.OpenAPI;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI platformOpenApi() {
        return OpenApiSupport.baseOpenApi(
                "CollabFlow Platform Service",
                "Organizations, workspaces, projects. All /api/** require X-API-Key.",
                "v1"
        );
    }
}
