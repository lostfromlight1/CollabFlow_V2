package com.collabflow.gateway.filter;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

class ImpersonationRulesTest {

    @Test
    void actorTokenCanGetButCannotPostProductRoutes() {
        assertThat(ImpersonationRules.allowed(HttpMethod.GET, "/api/v1/organizations", null, true)).isTrue();
        assertThat(ImpersonationRules.allowed(HttpMethod.POST, "/api/v1/tasks", null, true)).isFalse();
        assertThat(ImpersonationRules.allowed(HttpMethod.GET, "/api/v1/admin/users", null, true)).isFalse();
        assertThat(ImpersonationRules.allowed(
                HttpMethod.POST, "/api/v1/admin/sessions/impersonate/end", null, true
        )).isTrue();
        assertThat(ImpersonationRules.allowed(
                HttpMethod.GET, "/api/v1/realtime/ws", "websocket", true
        )).isFalse();
        assertThat(ImpersonationRules.allowed(HttpMethod.POST, "/api/v1/tasks", null, false)).isTrue();
    }
}
