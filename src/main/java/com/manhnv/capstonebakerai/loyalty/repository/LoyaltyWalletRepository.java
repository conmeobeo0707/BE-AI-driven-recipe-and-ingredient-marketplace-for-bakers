package com.manhnv.capstonebakerai.loyalty.repository;

import com.manhnv.capstonebakerai.loyalty.entity.LoyaltyWallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface LoyaltyWalletRepository extends JpaRepository<LoyaltyWallet, UUID> {

    @Query("select w.balance from LoyaltyWallet w where w.customerId = :customerId")
    Optional<Integer> findBalance(@Param("customerId") UUID customerId);

    @Modifying
    @Query(value = "insert into loyalty_wallets (customer_id) values (:customerId) on conflict (customer_id) do nothing",
            nativeQuery = true)
    void createIfMissing(@Param("customerId") UUID customerId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update LoyaltyWallet w
            set w.balance = w.balance + :points, w.updatedAt = current_timestamp
            where w.customerId = :customerId
            """)
    int credit(@Param("customerId") UUID customerId, @Param("points") int points);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update LoyaltyWallet w
            set w.balance = w.balance - :points, w.updatedAt = current_timestamp
            where w.customerId = :customerId and w.balance >= :points
            """)
    int debit(@Param("customerId") UUID customerId, @Param("points") int points);
}
