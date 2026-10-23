package com.collabflow.common.openapi;

import com.collabflow.common.security.ApiKeyFilter;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

/**
 * Shared OpenAPI / Swagger security setup for CollabFlow Java services.
 */
public final class OpenApiSupport {

    public static final String API_KEY_SCHEME = "ApiKey";
    public static final String BEARER_SCHEME = "BearerAuth";

    private OpenApiSupport() {}

    public static OpenAPI baseOpenApi(String title, String description, String version) {
        return new OpenAPI()
                .info(new Info().title(title).description(description).version(version))
                .components(new Components()
                        .addSecuritySchemes(API_KEY_SCHEME, new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name(ApiKeyFilter.HEADER_NAME)
                                .description("Client API key (env API_KEY)"))
                        .addSecuritySchemes(BEARER_SCHEME, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Access token from login/register")))
                .addSecurityItem(new SecurityRequirement().addList(API_KEY_SCHEME));
    }
}
