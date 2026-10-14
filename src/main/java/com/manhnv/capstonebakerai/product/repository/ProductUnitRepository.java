package com.manhnv.capstonebakerai.product.repository;

import com.manhnv.capstonebakerai.product.entities.ProductUnit;
import com.manhnv.capstonebakerai.product.entities.UnitStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductUnitRepository extends JpaRepository<ProductUnit, UUID> {
    List<ProductUnit> findByProductId(UUID productId);
    Optional<ProductUnit> findByUnitIdAndProductId(UUID unitId, UUID productId);
    Optional<ProductUnit> findByProductIdAndBaseTrueAndStatus(UUID productId, UnitStatus status);
}
