package com.manhnv.capstonebakerai.warehouse.repository;

import com.manhnv.capstonebakerai.warehouse.entity.StockLot;
import com.manhnv.capstonebakerai.warehouse.enums.LotStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
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
    List<StockLot> findByProductIdInAndStatusInOrderByProductIdAscExpiryDateAscCreatedAtAsc(
            Collection<UUID> productIds, Collection<LotStatus> statuses);

    @Query("select l from StockLot l where l.productId = :productId and l.status = :status " +
            "and l.currentQuantity > 0 and (l.expiryDate is null or l.expiryDate >= :today) " +
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
}
