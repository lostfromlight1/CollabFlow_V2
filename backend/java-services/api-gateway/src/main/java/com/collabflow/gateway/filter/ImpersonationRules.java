package com.collabflow.gateway.filter;

import org.springframework.http.HttpMethod;

/**
 * Read-only impersonation: a token with an {@code actor} claim may GET/HEAD,
 * and may POST only the end-impersonation route. WebSocket upgrades are refused.
 */
public final class ImpersonationRules {

    private ImpersonationRules() {}

    public static boolean allowed(HttpMethod method, String path, String upgradeHeader, boolean actorPresent) {
        if (!actorPresent) {
            return true;
        }
        if (path == null) {
            return false;
        }
        if (upgradeHeader != null && upgradeHeader.toLowerCase().contains("websocket")) {
            return false;
        }
        if (path.startsWith("/api/v1/realtime/ws")) {
            return false;
        }
        if (HttpMethod.GET.equals(method) || HttpMethod.HEAD.equals(method)) {
            if (path.startsWith("/api/v1/admin")) {
                return false;
            }
            return true;
        }
        return HttpMethod.POST.equals(method)
                && "/api/v1/admin/sessions/impersonate/end".equals(path);
    }
}
