package com.manhnv.capstonebakerai.warehouse.repository;

import com.manhnv.capstonebakerai.report.dto.response.MonthAmount;
import com.manhnv.capstonebakerai.warehouse.entity.StocktakeItem;
import com.manhnv.capstonebakerai.warehouse.enums.StocktakeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface StocktakeItemRepository extends JpaRepository<StocktakeItem, UUID> {
    List<StocktakeItem> findByNoteIdOrderByItemId(UUID noteId);
    @Query(value = "select exists (select 1 from stocktake_items i join stocktake_notes n on n.note_id=i.note_id " +
            "where i.product_id=:productId and n.status::text='DRAFT')", nativeQuery = true)
    boolean hasDraftStocktakeForProduct(@Param("productId") UUID productId);
    @Query(value = "select exists (select 1 from stocktake_items i join stocktake_notes n on n.note_id=i.note_id " +
            "where i.lot_id=:lotId and n.status::text='DRAFT')", nativeQuery = true)
    boolean hasDraftStocktakeForLot(@Param("lotId") UUID lotId);
    @Query("select distinct i.productId from StocktakeItem i where i.noteId=:noteId")
    List<UUID> findProductIdsByNoteId(@Param("noteId") UUID noteId);

    // Trả về số ÂM (tổng cost_amount của các dòng bị hụt), service sẽ đổi dấu
    @Query("""
    select new com.manhnv.capstonebakerai.report.dto.response.MonthAmount(
        extract(year from s.updatedAt),
        extract(month from s.updatedAt),
        sum(i.costAmount))
    from StocktakeItem i
    join StocktakeNote s on s.noteId = i.noteId
    where s.status = :status
      and i.costAmount < 0
      and s.updatedAt >= :from and s.updatedAt < :to
    group by extract(year from s.updatedAt), extract(month from s.updatedAt)
    """)
    List<MonthAmount> stocktakeLossByMonth(
            @Param("status") StocktakeStatus status,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);
}
