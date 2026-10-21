package com.manhnv.capstonebakerai.warehouse.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "stock_reservation_items", indexes = {
        @Index(name = "idx_stock_reservation_items_lot", columnList = "lot_id"),
        @Index(name = "idx_stock_reservation_items_reservation", columnList = "reservation_id")
})
@Getter @Setter @NoArgsConstructor
public class StockReservationItem {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "item_id")
    private UUID itemId;

    @Column(name = "reservation_id", nullable = false)
    private UUID reservationId;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "lot_id", nullable = false)
    private UUID lotId;

    @Column(name = "unit_id", nullable = false)
    private UUID unitId;

    @Column(name = "quantity", nullable = false, precision = 10, scale = 2)
    private BigDecimal quantity;
}
