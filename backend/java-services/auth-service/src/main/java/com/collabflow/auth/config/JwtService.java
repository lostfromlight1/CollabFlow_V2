package com.collabflow.auth.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private final JwtProperties properties;
    private final SecretKey key;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
        this.key = Keys.hmacShaKeyFor(properties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String createAccessToken(UUID userId, String email) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(properties.getAccessTokenMinutes() * 60);
        return sign(userId, email, null, now, exp);
    }

    /** 15-minute read-only token. {@code sub} is the target; {@code actor} is the admin. No refresh token. */
    public String createImpersonationToken(UUID targetUserId, String email, UUID actorId) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(15 * 60);
        return sign(targetUserId, email, actorId, now, exp);
    }

    private String sign(UUID userId, String email, UUID actorId, Instant now, Instant exp) {
        var builder = Jwts.builder()
                .subject(userId.toString())
                .claim("email", email)
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp));
        if (actorId != null) {
            builder.claim("actor", actorId.toString());
        }
        // Force HS256 so api-gateway (Nimbus HS256) can verify the same token
        return builder.signWith(key, Jwts.SIG.HS256).compact();
    }

    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public UUID userId(String token) {
        return UUID.fromString(parse(token).getSubject());
    }
}
