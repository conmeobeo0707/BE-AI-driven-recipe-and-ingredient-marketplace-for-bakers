package com.manhnv.capstonebakerai.warehouse.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Check;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "stock_reservation_items", indexes = {
        @Index(name = "idx_stock_reservation_items_lot", columnList = "lot_id"),
        @Index(name = "idx_stock_reservation_items_reservation", columnList = "reservation_id")
})
@Check(name = "ck_stock_reservation_items_quantity", constraints = "quantity > 0")
@Getter
@Setter
public class StockReservationItem {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "item_id")
    private UUID itemId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reservation_id", nullable = false)
    private StockReservation reservation;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "lot_id", nullable = false)
    private UUID lotId;

    @Column(name = "unit_id", nullable = false)
    private UUID unitId;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal quantity;
}
