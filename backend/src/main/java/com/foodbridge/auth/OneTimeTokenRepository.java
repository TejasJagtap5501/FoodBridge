package com.foodbridge.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface OneTimeTokenRepository extends JpaRepository<OneTimeToken, Long> {

    Optional<OneTimeToken> findByTokenHashAndPurpose(String tokenHash, TokenPurpose purpose);

    @Modifying
    @Query("UPDATE OneTimeToken t SET t.usedAt = CURRENT_TIMESTAMP WHERE t.userId = :userId AND t.purpose = :purpose AND t.usedAt IS NULL")
    int invalidateAll(@Param("userId") Long userId, @Param("purpose") TokenPurpose purpose);
}
