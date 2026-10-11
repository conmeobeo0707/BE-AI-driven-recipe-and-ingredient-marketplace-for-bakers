package com.manhnv.capstonebakerai.product.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "product_categories")
@Getter @Setter @NoArgsConstructor
public class ProductCategory {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "product_category_id")
    private UUID productCategoryId;
    @Column(name = "category_name", nullable = false)
    private String categoryName;
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
