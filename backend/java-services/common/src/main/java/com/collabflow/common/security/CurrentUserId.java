package com.collabflow.common.security;

import io.swagger.v3.oas.annotations.Parameter;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

/**
 * Injects the JWT principal as {@link java.util.UUID} (set by each service's JwtAuthenticationFilter).
 * Hidden from OpenAPI — clients send {@code Authorization: Bearer}, not a body field.
 */
@Target({ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@AuthenticationPrincipal
@Parameter(hidden = true)
public @interface CurrentUserId {
}
