package com.manhnv.capstonebakerai.warehouse.repository;

import com.manhnv.capstonebakerai.warehouse.entity.StockLot;
import com.manhnv.capstonebakerai.warehouse.enums.LotStatus;
import com.manhnv.capstonebakerai.warehouse.enums.StockReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;

public interface StockLotRepository extends JpaRepository<StockLot, UUID> {
    List<StockLot> findByLotIdIn(Collection<UUID> lotIds);
    List<StockLot> findByProductIdOrderByExpiryDateAscCreatedAtAsc(UUID productId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from StockLot l where l.lotId in :ids order by l.lotId")
    List<StockLot> lockByLotIds(@Param("ids") Collection<UUID> ids);
    @Query("select l from StockLot l where l.productId in :productIds and l.status in :statuses " +
            "and l.currentQuantity > 0 order by l.productId, l.expiryDate, l.createdAt")
    List<StockLot> findCountableLots(@Param("productIds") Collection<UUID> productIds,
            @Param("statuses") Collection<LotStatus> statuses);

    @Query("""
            select case when count(i) > 0 then true else false end
            from StockReservationItem i
            where i.lotId in :lotIds and i.reservation.status = :status
                and i.reservation.expiresAt > :now
            """)
    boolean hasActiveReservations(@Param("lotIds") Collection<UUID> lotIds,
            @Param("status") StockReservationStatus status, @Param("now") LocalDateTime now);

    @Query(value = """
            select exists (
                select 1 from stock_out_item i join stock_out_notes n on n.note_id = i.note_id
                where i.lot_id in (:lotIds) and n.type = 'DISPOSAL'
                    and n.status = 'PENDING' and n.approved_at is not null
            )
            """, nativeQuery = true)
    boolean hasApprovedDisposal(@Param("lotIds") Collection<UUID> lotIds);

    String STOCKTAKE_DAMAGE_HOLD = """
                from stocktake_items i join stocktake_notes n on n.note_id = i.note_id
                where n.status = 'DRAFT' and n.reviewed_at is not null and i.damaged_quantity > 0
            """;

    @Query(value = "select coalesce(sum(i.damaged_quantity), 0) " + STOCKTAKE_DAMAGE_HOLD
            + " and i.lot_id = :lotId and n.note_id <> :excludedStocktakeId", nativeQuery = true)
    BigDecimal sumStocktakeDamageHold(@Param("lotId") UUID lotId, @Param("excludedStocktakeId") UUID excludedStocktakeId);

    String SALEABLE_LOT = "l.productId = :productId and l.status = :status " +
            "and l.currentQuantity > 0 and (l.expiryDate is null or l.expiryDate >= :today)";

    @Query(value = """
            select coalesce(sum(l.current_quantity), 0) - coalesce(sum((
                select sum(i.quantity)
                from stock_reservation_items i
                join stock_reservations r on r.reservation_id = i.reservation_id
                where i.lot_id = l.lot_id and r.status = 'ACTIVE' and r.expires_at > :now
            )), 0)
                - coalesce(sum((
                    select sum(i.quantity)
                    from stock_out_item i join stock_out_notes n on n.note_id = i.note_id
                    where i.lot_id = l.lot_id and n.type = 'DISPOSAL'
                        and n.status = 'PENDING' and n.approved_at is not null
                        and not exists (select 1 from stocktake_notes s
                            where s.note_id = n.stocktake_note_id and s.reviewed_at is not null)
                )), 0) - coalesce(sum((
                    select sum(i.damaged_quantity)
            """ + STOCKTAKE_DAMAGE_HOLD + """
                    and i.lot_id = l.lot_id
                )), 0)
            from stock_lot l
            where l.product_id = :productId and l.status = 'STORAGE' and l.current_quantity > 0
                and (l.expiry_date is null or l.expiry_date >= :today)
                and not exists (select 1 from stocktake_items i join stocktake_notes n on n.note_id = i.note_id
                    where i.lot_id = l.lot_id and n.status = 'DRAFT' and n.reviewed_at is null)
            """, nativeQuery = true)
    BigDecimal sumSaleableQuantity(@Param("productId") UUID productId,
            @Param("today") LocalDate today, @Param("now") LocalDateTime now);

    @Query("select l from StockLot l where " + SALEABLE_LOT + " " +
            "order by case when l.expiryDate is null then 1 else 0 end, l.expiryDate asc, l.createdAt asc, l.lotId asc")
    List<StockLot> findSaleableLots(@Param("productId") UUID productId,
            @Param("status") LotStatus status, @Param("today") LocalDate today);

    @Query("select l from StockLot l where l.productId = :productId and l.status in :statuses " +
            "and l.currentQuantity > 0 order by l.expiryDate asc, l.createdAt asc")
    List<StockLot> findPositiveLotsByProduct(@Param("productId") UUID productId,
            @Param("statuses") Collection<LotStatus> statuses);

    @Query("select l from StockLot l where l.status = :status and l.currentQuantity > 0 " +
            "and l.expiryDate between :today and :until order by l.expiryDate asc")
    List<StockLot> findExpiringLots(@Param("status") LotStatus status,
            @Param("today") LocalDate today, @Param("until") LocalDate until);

    @Query("select l from StockLot l where l.status in :statuses and l.currentQuantity > 0 " +
            "and l.expiryDate < :today order by l.expiryDate asc, l.createdAt asc, l.lotId asc")
    List<StockLot> findExpiredLots(@Param("statuses") Collection<LotStatus> statuses,
            @Param("today") LocalDate today);
}
