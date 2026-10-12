package com.manhnv.capstonebakerai.warehouse.entity;

import com.manhnv.capstonebakerai.warehouse.enums.StocktakeStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity @Table(name = "stocktake_notes")
@Getter @Setter @NoArgsConstructor
public class StocktakeNote {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "note_id")
    private UUID noteId;
    @Column(nullable = false, unique = true, length = 100)
    private String code;
    @Column(name = "stocktake_date", nullable = false)
    private LocalDateTime stocktakeDate;
    @Column(name = "handle_by")
    private UUID handleBy;
    @Column(name = "created_by")
    private UUID createdBy;
    @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "stocktake_status_enum")
    private StocktakeStatus status = StocktakeStatus.DRAFT;
    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
