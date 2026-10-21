package com.manhnv.capstonebakerai.warehouse.repository;

import com.manhnv.capstonebakerai.warehouse.entity.StockReservationItem;
import com.manhnv.capstonebakerai.warehouse.enums.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface StockReservationItemRepository extends JpaRepository<StockReservationItem, UUID> {
    List<StockReservationItem> findByReservationIdOrderByItemId(UUID reservationId);

    @Query("select coalesce(sum(item.quantity), 0) from StockReservationItem item " +
            "join StockReservation reservation on reservation.reservationId = item.reservationId " +
            "where item.lotId = :lotId and reservation.status = :activeStatus and reservation.expiresAt > :now")
    BigDecimal sumActiveQuantityByLotId(@Param("lotId") UUID lotId,
                                        @Param("activeStatus") ReservationStatus activeStatus,
                                        @Param("now") LocalDateTime now);
}
