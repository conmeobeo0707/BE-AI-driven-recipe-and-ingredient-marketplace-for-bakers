package com.manhnv.capstonebakerai.warehouse.repository;

import com.manhnv.capstonebakerai.warehouse.entity.StocktakeItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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
}
