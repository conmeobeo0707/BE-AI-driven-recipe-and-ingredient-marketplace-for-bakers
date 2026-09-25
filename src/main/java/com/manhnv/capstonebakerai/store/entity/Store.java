package com.manhnv.capstonebakerai.store.entity;

import com.manhnv.capstonebakerai.identity.entity.User;
import com.manhnv.capstonebakerai.store.enums.StoreStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "stores")
@Getter
@Setter
@NoArgsConstructor
public class Store {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "store_id", nullable = false, updatable = false)
    private UUID storeId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal revenue = BigDecimal.ZERO;

    @Column(nullable = false, precision = 3, scale = 2)
    private BigDecimal rating = BigDecimal.ZERO;

    @Column(name = "is_verified", nullable = false)
    private boolean isVerified;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "logo_url", length = 2048)
    private String logoUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StoreStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
