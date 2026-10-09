package com.manhnv.capstonebakerai.product.repository;

import com.manhnv.capstonebakerai.product.entities.ProductCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface ProductCategoryRepository extends JpaRepository<ProductCategory, UUID> { }
