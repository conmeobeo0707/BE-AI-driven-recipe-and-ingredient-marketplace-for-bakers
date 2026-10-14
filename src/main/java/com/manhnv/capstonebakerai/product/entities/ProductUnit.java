package com.manhnv.capstonebakerai.product.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "product_units")
@Getter @Setter @NoArgsConstructor
public class ProductUnit {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "unit_id")
    private UUID unitId;
    @Column(name = "product_id", nullable = false)
    private UUID productId;
    @Column(name = "unit_name", nullable = false)
    private String unitName;
    @Column(name = "conversion_factor", nullable = false, precision = 10, scale = 2)
    private BigDecimal conversionFactor;
    @Column(name = "is_base")
    private boolean base;
    @Column(name = "is_sale")
    private boolean sale = true;
    @Column(name = "selling_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal sellingPrice;
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "unit_status_enum")
    private UnitStatus status = UnitStatus.ACTIVE;
    @Column(name = "weight_gram")
    private Integer weightGram;   // khối lượng của MỘT đơn vị bán, tính bằng gram
}
