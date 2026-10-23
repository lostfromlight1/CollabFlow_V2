package com.collabflow.user.service;

import com.collabflow.common.exception.BusinessException;
import com.collabflow.common.exception.ErrorCode;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Asks auth-service to revoke refresh tokens. Sessions live in auth;
 * user-service must not update that table itself.
 */
@Service
public class AuthSessionRevoker {

    private final RestClient restClient;

    public AuthSessionRevoker(@Value("${collabflow.auth.base-url:http://localhost:8081}") String baseUrl) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    public void revoke(UUID userId) {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Could not revoke sessions for deleted user");
        }
        var request = attrs.getRequest();
        try {
            restClient.post()
                    .uri("/api/v1/admin/users/{id}/sessions/revoke", userId)
                    .header("Authorization", request.getHeader("Authorization"))
                    .header("X-API-Key", request.getHeader("X-API-Key"))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, res) -> {
                        throw new BusinessException(ErrorCode.BAD_REQUEST, "Could not revoke sessions for deleted user");
                    })
                    .toBodilessEntity();
        } catch (BusinessException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Could not revoke sessions for deleted user");
        }
    }
}
