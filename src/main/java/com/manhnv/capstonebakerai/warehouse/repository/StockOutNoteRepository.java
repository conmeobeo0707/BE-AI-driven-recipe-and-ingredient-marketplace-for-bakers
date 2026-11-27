package com.manhnv.capstonebakerai.warehouse.repository;
import com.manhnv.capstonebakerai.order.enums.OrderStatus;
import com.manhnv.capstonebakerai.report.dto.response.MonthAmount;
import com.manhnv.capstonebakerai.warehouse.entity.StockOutNote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.manhnv.capstonebakerai.warehouse.enums.NoteStatus;
import com.manhnv.capstonebakerai.warehouse.enums.StockOutType;
public interface StockOutNoteRepository extends JpaRepository<StockOutNote, UUID> {
    Optional<StockOutNote> findByReferenceOrderIdAndType(UUID referenceOrderId, StockOutType type);
    Optional<StockOutNote> findByStocktakeNoteIdAndStatus(UUID stocktakeNoteId, NoteStatus status);
    @Query("select n.stocktakeNoteId from StockOutNote n where n.noteId = :id")
    Optional<UUID> findStocktakeNoteId(@Param("id") UUID id);
    java.util.Optional<StockOutNote> findByCodeIgnoreCase(String code);
    boolean existsByCode(String code);
    Page<StockOutNote> findByStatus(NoteStatus status, Pageable pageable);
    Page<StockOutNote> findByType(StockOutType type, Pageable pageable);
    Page<StockOutNote> findByStatusAndType(NoteStatus status, StockOutType type, Pageable pageable);
    @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select n from StockOutNote n where n.noteId=:id")
    Optional<StockOutNote> lockById(@Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select n from StockOutNote n where n.referenceOrderId = :orderId and n.type = :type")
    Optional<StockOutNote> lockByReferenceOrderIdAndType(@Param("orderId") UUID orderId,
            @Param("type") StockOutType type);
    // Giá vốn: tính theo tháng giao hàng của đơn
    @Query("""
    select new com.manhnv.capstonebakerai.report.dto.response.MonthAmount(
        extract(year from o.deliveredAt),
        extract(month from o.deliveredAt),
        sum(n.totalCost))
    from StockOutNote n
    join Order o on o.id = n.referenceOrderId
    where n.type = :type
      and n.status = :noteStatus
      and o.status = :orderStatus
      and o.deliveredAt >= :from and o.deliveredAt < :to
    group by extract(year from o.deliveredAt), extract(month from o.deliveredAt)
    """)
    List<MonthAmount> cogsByMonth(@Param("type") StockOutType type,
                                  @Param("noteStatus") NoteStatus noteStatus,
                                  @Param("orderStatus") OrderStatus orderStatus,
                                  @Param("from") LocalDateTime from,
                                  @Param("to") LocalDateTime to);

    // Hao hụt do hủy hàng
    @Query("""
    select new com.manhnv.capstonebakerai.report.dto.response.MonthAmount(
        extract(year from n.updatedAt),
        extract(month from n.updatedAt),
        sum(n.totalCost))
    from StockOutNote n
    where n.type = :type
      and n.status = :noteStatus
      and n.updatedAt >= :from and n.updatedAt < :to
    group by extract(year from n.updatedAt), extract(month from n.updatedAt)
    """)
    List<MonthAmount> disposalByMonth(@Param("type") StockOutType type,
                                      @Param("noteStatus") NoteStatus noteStatus,
                                      @Param("from") LocalDateTime from,
                                      @Param("to") LocalDateTime to);
}
