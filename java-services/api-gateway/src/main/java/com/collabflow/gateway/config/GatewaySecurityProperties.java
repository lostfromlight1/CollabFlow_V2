package com.collabflow.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "collabflow")
public class GatewaySecurityProperties {

    private final Security security = new Security();
    private final Jwt jwt = new Jwt();

    public Security getSecurity() {
        return security;
    }

    public Jwt getJwt() {
        return jwt;
    }

    public static class Security {
        private String apiKey = "";

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }
    }

    public static class Jwt {
        private String secret = "";

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }
    }
}
