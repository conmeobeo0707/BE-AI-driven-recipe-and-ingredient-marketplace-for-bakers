package com.manhnv.capstonebakerai.product.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "unit_price_history")
@Getter @Setter @NoArgsConstructor
public class UnitPriceHistory {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "unit_history_id")
    private UUID unitHistoryId;
    @Column(name = "unit_id", nullable = false)
    private UUID unitId;
    @Column(name = "updated_by")
    private UUID updatedBy;
    @Column(name = "selling_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal sellingPrice;
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
