package com.collabflow.auth.config;

import com.collabflow.common.openapi.OpenApiSupport;
import io.swagger.v3.oas.models.OpenAPI;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI authOpenApi() {
        return OpenApiSupport.baseOpenApi(
                "CollabFlow Auth Service",
                "Register, login, refresh, logout, and current user. "
                        + "All /api/** calls require header X-API-Key. "
                        + "Protected routes also need Authorization: Bearer <accessToken>.",
                "v1"
        );
    }
}
