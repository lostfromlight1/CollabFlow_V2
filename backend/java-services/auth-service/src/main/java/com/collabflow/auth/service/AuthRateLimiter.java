package com.collabflow.auth.service;

import com.collabflow.auth.config.RateLimitProperties;
import com.collabflow.common.exception.BusinessException;
import com.collabflow.common.exception.ErrorCode;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Redis counter rate limit (INCR + TTL).
 * Fail-open only in {@code dev}/{@code local}; fail-closed otherwise.
 */
@Service
public class AuthRateLimiter {

    private static final Logger log = LoggerFactory.getLogger(AuthRateLimiter.class);

    private final StringRedisTemplate redis;
    private final RateLimitProperties properties;
    private final Environment environment;

    public AuthRateLimiter(
            StringRedisTemplate redis,
            RateLimitProperties properties,
            Environment environment
    ) {
        this.redis = redis;
        this.properties = properties;
        this.environment = environment;
    }

    public void checkLogin(String clientKey) {
        check("login", clientKey, properties.getLoginMaxAttempts(), properties.getLoginWindowSeconds());
    }

    public void checkRegister(String clientKey) {
        check("register", clientKey, properties.getRegisterMaxAttempts(), properties.getRegisterWindowSeconds());
    }

    public void checkRefresh(String clientKey) {
        check("refresh", clientKey, properties.getRefreshMaxAttempts(), properties.getRefreshWindowSeconds());
    }

    public void checkResendVerify(String clientKey) {
        check("resend_verify", clientKey, properties.getResendVerifyMaxAttempts(), properties.getResendVerifyWindowSeconds());
    }

    public void checkForgotPassword(String clientKey) {
        check("forgot_password", clientKey, properties.getForgotPasswordMaxAttempts(), properties.getForgotPasswordWindowSeconds());
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
            if (isDevLike()) {
                log.warn("Redis rate limit unavailable for action={} — allowing (dev): {}", action, e.toString());
                return;
            }
            log.error("Redis rate limit unavailable for action={} — rejecting (fail-closed)", action, e);
            throw new BusinessException(
                    ErrorCode.INTERNAL_ERROR,
                    "Rate limiting unavailable; try again later"
            );
        }
    }

    private boolean isDevLike() {
        return environment.acceptsProfiles(Profiles.of("dev", "local"))
                || environment.getActiveProfiles().length == 0;
    }

    private static String sanitize(String clientKey) {
        if (clientKey == null || clientKey.isBlank()) {
            return "unknown";
        }
        return clientKey.replaceAll("[^a-zA-Z0-9.:_-]", "_");
    }
}
