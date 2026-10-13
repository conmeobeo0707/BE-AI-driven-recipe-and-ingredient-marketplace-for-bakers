package com.manhnv.capstonebakerai.warehouse.entity;

import com.manhnv.capstonebakerai.warehouse.enums.NoteStatus;
import com.manhnv.capstonebakerai.warehouse.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity @Table(name = "stock_in_notes")
@Getter @Setter @NoArgsConstructor
public class StockInNote {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "note_id")
    private UUID noteId;
    @Column(nullable = false, unique = true, length = 100)
    private String code;
    @Column(name = "supplier_id", nullable = false)
    private UUID supplierId;
    @Column(name = "receipt_date", nullable = false)
    private LocalDateTime receiptDate;
    @Column(name = "total_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount;
    @Column(name = "paid_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal paidAmount;
    @Column(name = "debt_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal debtAmount;
    @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "payment_status", columnDefinition = "payment_status_enum")
    private PaymentStatus paymentStatus;
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
