package com.manhnv.capstonebakerai.warehouse.entity;

import com.manhnv.capstonebakerai.warehouse.enums.LotStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity @Table(name = "stock_lot")
@Getter @Setter @NoArgsConstructor
public class StockLot {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "lot_id")
    private UUID lotId;
    @Column(name = "product_id", nullable = false)
    private UUID productId;
    @Column(name = "stock_in_note_id", nullable = false)
    private UUID stockInNoteId;
    @Column(name = "lot_code", nullable = false, length = 100)
    private String lotCode;
    @Column(name = "initial_quantity", nullable = false, precision = 10, scale = 2)
    private BigDecimal initialQuantity;
    @Column(name = "current_quantity", nullable = false, precision = 10, scale = 2)
    private BigDecimal currentQuantity;
    @Column(name = "unit_cost", nullable = false, precision = 15, scale = 2)
    private BigDecimal unitCost;
    @Column(name = "expiry_date")
    private LocalDate expiryDate;
    @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "lot_status_enum")
    private LotStatus status = LotStatus.STORAGE;
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
