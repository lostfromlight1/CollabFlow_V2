package com.collabflow.user.config;

import com.collabflow.common.openapi.OpenApiSupport;
import io.swagger.v3.oas.models.OpenAPI;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI userOpenApi() {
        return OpenApiSupport.baseOpenApi(
                "CollabFlow User Service",
                "User profiles and admin user create. Requires X-API-Key + Bearer JWT.",
                "v1"
        );
    }
}
