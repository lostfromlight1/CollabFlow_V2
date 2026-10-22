package com.collabflow.common.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Puts {@code apiId} (from {@link ApiId}) and {@code userId} (JWT principal UUID when present) into MDC.
 */
public class ApiIdMdcInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (handler instanceof HandlerMethod method) {
            ApiId apiId = method.getMethodAnnotation(ApiId.class);
            if (apiId != null) {
                MDC.put(MdcKeys.API_ID, apiId.value());
            }
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof UUID userId) {
            MDC.put(MdcKeys.USER_ID, userId.toString());
        }
        return true;
    }

    @Override
    public void afterCompletion(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
            Exception ex
    ) {
        MDC.remove(MdcKeys.API_ID);
        MDC.remove(MdcKeys.USER_ID);
    }
}
