package com.manhnv.capstonebakerai.payment.repository;

import com.manhnv.capstonebakerai.payment.entity.Payment;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository  extends JpaRepository<Payment, UUID> {
    Optional<Payment> findByOrderId(UUID orderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)       // chống 2 webhook trùng chạy song song
    @Query("select p from Payment p where p.externalId = :externalId")
    Optional<Payment> findByExternalIdForUpdate(String externalId);
}
