package com.manhnv.capstonebakerai.identity.repository;

import com.manhnv.capstonebakerai.identity.entity.VerificationCode;
import com.manhnv.capstonebakerai.identity.enums.VerificationPurpose;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface VerificationCodeRepository extends JpaRepository<VerificationCode, UUID> {
    Optional<VerificationCode> findByUserUserIdAndPurpose(UUID userId, VerificationPurpose purpose);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select code from VerificationCode code where code.user.userId = :userId and code.purpose = :purpose")
    Optional<VerificationCode> findByUserIdAndPurposeForUpdate(
            @Param("userId") UUID userId, @Param("purpose") VerificationPurpose purpose);
}
