package com.collabflow.auth.repository;

import com.collabflow.auth.model.EmailToken;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailTokenRepository extends JpaRepository<EmailToken, UUID> {

    Optional<EmailToken> findByTokenAndPurposeAndUsedAtIsNull(String token, String purpose);
}
