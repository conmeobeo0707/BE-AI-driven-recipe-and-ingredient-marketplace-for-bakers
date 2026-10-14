package com.manhnv.capstonebakerai.warehouse.repository;
import com.manhnv.capstonebakerai.warehouse.entity.StockInItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;
public interface StockInItemRepository extends JpaRepository<StockInItem, UUID> {
    List<StockInItem> findByNoteIdOrderByItemId(UUID noteId);
}
