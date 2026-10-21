package com.manhnv.capstonebakerai.warehouse.repository;

import com.manhnv.capstonebakerai.warehouse.entity.StockReservation;
import com.manhnv.capstonebakerai.warehouse.enums.ReservationStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface StockReservationRepository extends JpaRepository<StockReservation, UUID> {
    Optional<StockReservation> findByOrderId(UUID orderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select reservation from StockReservation reservation where reservation.orderId = :orderId")
    Optional<StockReservation> lockByOrderId(@Param("orderId") UUID orderId);

    @Modifying
    @Query("update StockReservation reservation set reservation.status = :expiredStatus, reservation.updatedAt = :now " +
            "where reservation.status = :activeStatus and reservation.expiresAt <= :now")
    int expirePastDue(@Param("activeStatus") ReservationStatus activeStatus,
                      @Param("expiredStatus") ReservationStatus expiredStatus,
                      @Param("now") LocalDateTime now);
}
