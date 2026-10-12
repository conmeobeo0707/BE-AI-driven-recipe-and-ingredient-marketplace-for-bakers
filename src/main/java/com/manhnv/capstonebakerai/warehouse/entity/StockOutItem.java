package com.manhnv.capstonebakerai.warehouse.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.util.UUID;

@Entity @Table(name = "stock_out_item")
@Getter @Setter @NoArgsConstructor
public class StockOutItem {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "item_id")
    private UUID itemId;
    @Column(name = "note_id", nullable = false)
    private UUID noteId;
    @Column(name = "product_id", nullable = false)
    private UUID productId;
    @Column(name = "lot_id")
    private UUID lotId;
    @Column(name = "unit_id", nullable = false)
    private UUID unitId;
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal quantity;
    @Column(name = "unit_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal unitPrice;
    @Column(name = "total_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalPrice;
}
