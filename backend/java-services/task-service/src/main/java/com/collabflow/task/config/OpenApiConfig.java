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
                "Workflows + tasks. Requires X-API-Key + Bearer JWT.",
                "v1"
        );
    }
}
