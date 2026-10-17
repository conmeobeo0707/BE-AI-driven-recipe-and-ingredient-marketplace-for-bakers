package com.manhnv.capstonebakerai.warehouse.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity @Table(name = "stock_in_item")
@Getter @Setter @NoArgsConstructor
public class StockInItem {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "item_id")
    private UUID itemId;
    @Column(name = "note_id", nullable = false)
    private UUID noteId;
    @Column(name = "product_id", nullable = false)
    private UUID productId;
    @Column(name = "unit_id", nullable = false)
    private UUID unitId;
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal quantity;
    @Column(name = "conversion_factor", nullable = false, precision = 10, scale = 2)
    private BigDecimal conversionFactor;
    @Column(name = "unit_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal unitPrice;
    @Column(name = "total_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalPrice;
    @Column(name = "expiry_date")
    private LocalDate expiryDate;
}
