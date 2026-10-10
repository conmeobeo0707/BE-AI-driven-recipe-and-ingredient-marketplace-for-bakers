package com.manhnv.capstonebakerai.identity.repository;

import com.manhnv.capstonebakerai.identity.entity.RefreshToken;
import com.manhnv.capstonebakerai.user.entity.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Modifying;

import java.time.LocalDateTime;

import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select token from RefreshToken token where token.tokenHash = :tokenHash")
    Optional<RefreshToken> findByTokenHashForUpdate(
            @Param("tokenHash") String tokenHash
    );

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Modifying
    @Query("update RefreshToken token set token.revokedAt = :revokedAt " +
            "where token.user = :user and token.revokedAt is null")
    int revokeAllActiveByUser(@Param("user") User user,
                              @Param("revokedAt") LocalDateTime revokedAt);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select token from RefreshToken token where token.refreshTokenId = :id")
    Optional<RefreshToken> findByIdForUpdate(@Param("id") UUID id);
}
