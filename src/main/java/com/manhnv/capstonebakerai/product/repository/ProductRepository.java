package com.manhnv.capstonebakerai.product.repository;

import com.manhnv.capstonebakerai.product.entities.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID>, JpaSpecificationExecutor<Product> {
    boolean existsBySkuIgnoreCase(String sku);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Product p where p.productId in :ids order by p.productId")
    List<Product> lockProducts(@Param("ids") Collection<UUID> ids);

    List<Product> findByComboFalseAndStatusAndStockQuantityLessThanEqual(
            com.manhnv.capstonebakerai.product.enums.ProductStatus status, java.math.BigDecimal threshold);

    @Query("select p from Product p where p.combo=false and p.status=:status and p.stockQuantity <= p.minStock order by p.name")
    List<Product> findLowStockProducts(@Param("status") com.manhnv.capstonebakerai.product.enums.ProductStatus status);
}
