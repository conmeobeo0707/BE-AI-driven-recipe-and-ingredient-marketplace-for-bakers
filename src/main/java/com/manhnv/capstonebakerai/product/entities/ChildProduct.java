package com.manhnv.capstonebakerai.product.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "children_products")
@Getter @Setter @NoArgsConstructor
public class ChildProduct {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "parent_product_id", nullable = false)
    private UUID parentProductId;
    @Column(name = "children_product_id", nullable = false)
    private UUID childrenProductId;
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal quantity;
}
