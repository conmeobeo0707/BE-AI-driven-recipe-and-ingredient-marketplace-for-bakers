package com.manhnv.capstonebakerai.loyalty.repository;

import com.manhnv.capstonebakerai.loyalty.entity.LoyaltyTransaction;
import com.manhnv.capstonebakerai.loyalty.enums.LoyaltyTransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface LoyaltyTransactionRepository extends JpaRepository<LoyaltyTransaction, UUID> {

    Page<LoyaltyTransaction> findByCustomer_IdOrderByCreatedAtDesc(UUID customerId, Pageable pageable);

    //Use for checking if a customer has already received a WELCOME bonus, or if an order has already been credited with ORDER_EARN points.
    boolean existsByCustomer_IdAndType(UUID customerId, LoyaltyTransactionType type);

    boolean existsByOrder_IdAndType(UUID orderId, LoyaltyTransactionType type);
}
