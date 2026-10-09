package com.manhnv.capstonebakerai.product.repository;

import com.manhnv.capstonebakerai.product.entities.ChildProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface ChildProductRepository extends JpaRepository<ChildProduct, UUID> {
    List<ChildProduct> findByParentProductId(UUID parentProductId);
    void deleteByParentProductId(UUID parentProductId);
}
