package com.collabflow.common.security;

import com.collabflow.common.exception.ErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Rejects {@code /api/**} requests without a valid {@code X-API-Key} header.
 * Swagger, actuator, and non-API paths are skipped.
 *
 * <p>Writes JSON without injecting {@code ObjectMapper} — servlet filter beans
 * can register before Jackson auto-config runs (Boot 4 / Tomcat init order).
 */
public class ApiKeyFilter extends OncePerRequestFilter {

    public static final String HEADER_NAME = "X-API-Key";

    private static final Logger log = LoggerFactory.getLogger(ApiKeyFilter.class);

    private final ApiKeyProperties properties;

    public ApiKeyFilter(ApiKeyProperties properties) {
        this.properties = properties;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!properties.isApiKeyEnabled()) {
            return true;
        }
        String path = request.getRequestURI();
        if (path == null) {
            return true;
        }
        return !path.startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String expected = properties.getApiKey();
        if (!StringUtils.hasText(expected)) {
            log.error("collabflow.security.api-key / API_KEY is not configured — rejecting {}", request.getRequestURI());
            writeUnauthorized(request, response, "API key is not configured on the server");
            return;
        }

        String provided = request.getHeader(HEADER_NAME);
        if (!StringUtils.hasText(provided) || !expected.equals(provided)) {
            log.warn("Missing or invalid {} on {} {}", HEADER_NAME, request.getMethod(), request.getRequestURI());
            writeUnauthorized(request, response, "Invalid or missing X-API-Key");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void writeUnauthorized(HttpServletRequest request, HttpServletResponse response, String message)
            throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        String path = request.getRequestURI() != null ? request.getRequestURI() : "";
        String body = """
                {"success":false,"message":"%s","errorCode":"%s","path":"%s","timestamp":"%s"}
                """.formatted(
                escapeJson(message),
                ErrorCode.UNAUTHORIZED.getCode(),
                escapeJson(path),
                Instant.now()
        ).trim();
        response.getOutputStream().write(body.getBytes(StandardCharsets.UTF_8));
    }

    private static String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
