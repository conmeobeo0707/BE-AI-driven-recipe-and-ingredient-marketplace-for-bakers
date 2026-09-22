package com.manhnv.capstonebakerai.identity.repository;

import com.manhnv.capstonebakerai.identity.entity.VerificationCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface VerificationCodeRepository extends JpaRepository<VerificationCode, UUID> {
    Optional<VerificationCode> findByUserUserId(UUID userId);
}
