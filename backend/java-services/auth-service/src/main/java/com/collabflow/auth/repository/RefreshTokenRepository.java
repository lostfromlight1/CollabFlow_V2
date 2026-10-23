package com.collabflow.auth.repository;

import com.collabflow.auth.model.RefreshToken;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByTokenAndRevokedFalse(String token);

    List<RefreshToken> findByUser_IdAndRevokedFalseOrderByCreatedAtDesc(UUID userId);

    @Modifying
    @Query("UPDATE RefreshToken t SET t.revoked = true, t.revokedAt = CURRENT_TIMESTAMP "
            + "WHERE t.user.id = :userId AND t.revoked = false")
    int revokeAllActiveForUser(@Param("userId") UUID userId);
}
