package com.manhnv.capstonebakerai.warehouse.entity;

import com.manhnv.capstonebakerai.warehouse.enums.ReservationStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "stock_reservations", uniqueConstraints = @UniqueConstraint(name = "uq_stock_reservations_order", columnNames = "order_id"),
        indexes = @Index(name = "idx_stock_reservations_status_expiry", columnList = "status, expires_at"))
@Getter @Setter @NoArgsConstructor
public class StockReservation {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "reservation_id")
    private UUID reservationId;

    @Column(name = "order_id", nullable = false, unique = true)
    private UUID orderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ReservationStatus status = ReservationStatus.ACTIVE;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();
}
