package com.manhnv.capstonebakerai.warehouse.entity;

import com.manhnv.capstonebakerai.warehouse.enums.StockTransactionRefType;
import com.manhnv.capstonebakerai.warehouse.enums.StockTransactionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "stock_transactions", indexes = {
        @Index(name = "idx_stock_transactions_product_created", columnList = "product_id, created_at"),
        @Index(name = "idx_stock_transactions_lot_created", columnList = "stock_lot_id, created_at"),
        @Index(name = "idx_stock_transactions_reference", columnList = "ref_type, ref_id")
})
@Getter
@Setter
@NoArgsConstructor
public class StockTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "stock_lot_id", nullable = false)
    private UUID stockLotId;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 32)
    private StockTransactionType transactionType;

    @Column(name = "quantity_change", nullable = false, precision = 10, scale = 2)
    private BigDecimal quantityChange;

    @Column(name = "balance_after", nullable = false, precision = 10, scale = 2)
    private BigDecimal balanceAfter;

    @Enumerated(EnumType.STRING)
    @Column(name = "ref_type", nullable = false, length = 32)
    private StockTransactionRefType refType;

    @Column(name = "ref_id", nullable = false)
    private UUID refId;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void setCreationTime() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
