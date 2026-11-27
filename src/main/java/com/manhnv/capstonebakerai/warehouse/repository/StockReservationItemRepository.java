package com.manhnv.capstonebakerai.warehouse.repository;

import com.manhnv.capstonebakerai.warehouse.entity.StockReservationItem;
import com.manhnv.capstonebakerai.warehouse.enums.StockReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface StockReservationItemRepository extends JpaRepository<StockReservationItem, UUID> {
    List<StockReservationItem> findByReservationReservationIdOrderByProductIdAscLotIdAsc(UUID reservationId);

    @Query("""
            select sum(i.quantity) from StockReservationItem i
            where i.lotId = :lotId and i.reservation.status = :status
                and i.reservation.expiresAt > :now
            """)
    BigDecimal sumHeldQuantity(UUID lotId, StockReservationStatus status, LocalDateTime now);
}
