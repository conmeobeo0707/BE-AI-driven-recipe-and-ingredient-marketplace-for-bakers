package com.manhnv.capstonebakerai.product.entities;

import com.manhnv.capstonebakerai.product.enums.ProductStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "products")
@Getter @Setter @NoArgsConstructor
public class Product {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "product_id")
    private UUID productId;

    @Column(nullable = false, unique = true, length = 100)
    private String sku;
    @Column(nullable = false, length = 255)
    private String name;
    @Column(columnDefinition = "text")
    private String description;
    @Column(name = "category_id")
    private UUID categoryId;
    @Column(name = "img_url", columnDefinition = "text")
    private String imgUrl;
    @Column(name = "discount_percent", precision = 5, scale = 2)
    private BigDecimal discountPercent = BigDecimal.ZERO;
    @Column(name = "storage_guide", columnDefinition = "text")
    private String storageGuide;
    @Column(name = "min_stock", precision = 10, scale = 2)
    private BigDecimal minStock = BigDecimal.ZERO;
    @Column(name = "max_stock", precision = 10, scale = 2)
    private BigDecimal maxStock;
    @Column(name = "stock_quantity", precision = 10, scale = 2)
    private BigDecimal stockQuantity = BigDecimal.ZERO;
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "product_status_enum")
    private ProductStatus status = ProductStatus.ACTIVE;
    @Column(length = 255)
    private String brand;
    @Column(name = "is_combo", nullable = false)
    private boolean combo;
    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @PreUpdate void onUpdate() { updatedAt = LocalDateTime.now(); }
}
