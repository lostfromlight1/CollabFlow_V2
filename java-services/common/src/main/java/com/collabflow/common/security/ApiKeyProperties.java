package com.collabflow.common.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Client API key (header {@code X-API-Key}). Distinct from JWT user auth.
 * Bind from env {@code API_KEY} via {@code collabflow.security.api-key}.
 */
@ConfigurationProperties(prefix = "collabflow.security")
public class ApiKeyProperties {

    /**
     * Expected value of the {@code X-API-Key} header.
     */
    private String apiKey = "";

    /**
     * When false, filter is a no-op (tests only). Default true.
     */
    private boolean apiKeyEnabled = true;

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public boolean isApiKeyEnabled() {
        return apiKeyEnabled;
    }

    public void setApiKeyEnabled(boolean apiKeyEnabled) {
        this.apiKeyEnabled = apiKeyEnabled;
    }
}
