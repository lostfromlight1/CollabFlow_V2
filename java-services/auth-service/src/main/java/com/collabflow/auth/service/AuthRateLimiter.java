package com.collabflow.auth.service;

import com.collabflow.auth.config.RateLimitProperties;
import com.collabflow.common.exception.BusinessException;
import com.collabflow.common.exception.ErrorCode;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Redis counter rate limit (INCR + TTL).
 *
 * <p>Key pattern: {@code rate_limit:auth:{action}:{clientKey}} → count, expires after window.
 * Java analogy: like a Guava RateLimiter, but shared across all auth-service instances.
 */
@Service
public class AuthRateLimiter {

    private static final Logger log = LoggerFactory.getLogger(AuthRateLimiter.class);

    private final StringRedisTemplate redis;
    private final RateLimitProperties properties;

    public AuthRateLimiter(StringRedisTemplate redis, RateLimitProperties properties) {
        this.redis = redis;
        this.properties = properties;
    }

    public void checkLogin(String clientKey) {
        check(
                "login",
                clientKey,
                properties.getLoginMaxAttempts(),
                properties.getLoginWindowSeconds()
        );
    }

    public void checkRegister(String clientKey) {
        check(
                "register",
                clientKey,
                properties.getRegisterMaxAttempts(),
                properties.getRegisterWindowSeconds()
        );
    }

    private void check(String action, String clientKey, int max, int windowSeconds) {
        String key = "rate_limit:auth:" + action + ":" + sanitize(clientKey);
        try {
            Long count = redis.opsForValue().increment(key);
            if (count != null && count == 1L) {
                redis.expire(key, Duration.ofSeconds(windowSeconds));
            }
            if (count != null && count > max) {
                log.warn("Rate limit exceeded action={} key={}", action, key);
                throw new BusinessException(
                        ErrorCode.FORBIDDEN,
                        "Too many " + action + " attempts. Try again later."
                );
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            // Fail open: local/dev without Redis should still work; log loudly.
            log.warn("Redis rate limit unavailable for action={} — allowing request: {}", action, e.toString());
        }
    }

    private static String sanitize(String clientKey) {
        if (clientKey == null || clientKey.isBlank()) {
            return "unknown";
        }
        return clientKey.replaceAll("[^a-zA-Z0-9.:_-]", "_");
    }
}
