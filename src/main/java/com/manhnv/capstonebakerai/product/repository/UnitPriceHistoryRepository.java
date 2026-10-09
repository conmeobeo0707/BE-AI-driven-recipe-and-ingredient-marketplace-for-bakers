package com.manhnv.capstonebakerai.product.repository;

import com.manhnv.capstonebakerai.product.entities.UnitPriceHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface UnitPriceHistoryRepository extends JpaRepository<UnitPriceHistory, UUID> {
    List<UnitPriceHistory> findByUnitIdOrderByCreatedAtDesc(UUID unitId);
}
