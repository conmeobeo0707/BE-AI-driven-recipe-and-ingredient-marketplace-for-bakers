package com.manhnv.capstonebakerai.warehouse.entity;

import com.manhnv.capstonebakerai.warehouse.enums.NoteStatus;
import com.manhnv.capstonebakerai.warehouse.enums.StockOutType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity @Table(name = "stock_out_notes")
@Getter @Setter @NoArgsConstructor
public class StockOutNote {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "note_id")
    private UUID noteId;
    @Column(nullable = false, unique = true, length = 100)
    private String code;
    @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "stock_out_type_enum")
    private StockOutType type;
    @Column(name = "reference_order_id")
    private UUID referenceOrderId;
    @Column(name = "total_cost", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalCost = BigDecimal.ZERO;
    @Column(columnDefinition = "text")
    private String note;
    @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "note_status_enum")
    private NoteStatus status = NoteStatus.PENDING;
    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
