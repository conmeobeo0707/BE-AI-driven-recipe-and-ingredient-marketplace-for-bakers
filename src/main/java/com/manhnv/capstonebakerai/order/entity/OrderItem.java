package com.manhnv.capstonebakerai.order.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "order_items")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    // Không FK sang module product để tránh phụ thuộc cứng, đơn cũ vẫn giữ nguyên khi sản phẩm bị sửa/xóa
    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "product_unit_id", nullable = false)
    private UUID productUnitId;

    // ===== Snapshot tại thời điểm đặt =====
    @Column(name = "product_name", nullable = false, length = 255)
    private String productName;

    @Column(name = "sku", length = 100)
    private String sku;

    @Column(name = "unit_name", nullable = false, length = 100)
    private String unitName;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "unit_price", nullable = false, precision = 19, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "line_total", nullable = false, precision = 19, scale = 2)
    private BigDecimal lineTotal;

    @Column(name = "weight_gram")
    private int weightGram;
}
