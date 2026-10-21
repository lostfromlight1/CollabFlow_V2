package com.collabflow.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * Registers {@link ApiKeyFilter} for every service that scans {@code com.collabflow.common}.
 */
@Configuration
@EnableConfigurationProperties(ApiKeyProperties.class)
public class CommonSecurityConfiguration {

    @Bean
    ApiKeyFilter apiKeyFilter(ApiKeyProperties properties, ObjectMapper objectMapper) {
        return new ApiKeyFilter(properties, objectMapper);
    }

    @Bean
    FilterRegistrationBean<ApiKeyFilter> apiKeyFilterRegistration(ApiKeyFilter apiKeyFilter) {
        FilterRegistrationBean<ApiKeyFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(apiKeyFilter);
        registration.addUrlPatterns("/api/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        registration.setName("collabflowApiKeyFilter");
        return registration;
    }
}
