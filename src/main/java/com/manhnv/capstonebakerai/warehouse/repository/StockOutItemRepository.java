package com.manhnv.capstonebakerai.warehouse.repository;
import com.manhnv.capstonebakerai.warehouse.entity.StockOutItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;
public interface StockOutItemRepository extends JpaRepository<StockOutItem, UUID> {
    List<StockOutItem> findByNoteIdOrderByItemId(UUID noteId);
    void deleteByNoteId(UUID noteId);
}
