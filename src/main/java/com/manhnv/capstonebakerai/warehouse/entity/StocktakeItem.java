package com.manhnv.capstonebakerai.warehouse.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity @Table(name = "stocktake_items")
@Getter @Setter @NoArgsConstructor
public class StocktakeItem {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "item_id")
    private UUID itemId;
    @Column(name = "note_id", nullable = false)
    private UUID noteId;
    @Column(name = "product_id", nullable = false)
    private UUID productId;
    @Column(name = "lot_id", nullable = false)
    private UUID lotId;
    @Column(name = "system_quantity", nullable = false, precision = 10, scale = 2)
    private BigDecimal systemQuantity;
    @Column(name = "actual_quantity", precision = 10, scale = 2)
    private BigDecimal actualQuantity;
    @Column(name = "diff_quantity", precision = 10, scale = 2)
    private BigDecimal diffQuantity;
    @Column(name = "unit_cost", nullable = false, precision = 15, scale = 2)
    private BigDecimal unitCost;
    @Column(name = "cost_amount", precision = 15, scale = 2)
    private BigDecimal costAmount;
    @Column(length = 255)
    private String reason;
    @Column(columnDefinition = "text")
    private String note;
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
