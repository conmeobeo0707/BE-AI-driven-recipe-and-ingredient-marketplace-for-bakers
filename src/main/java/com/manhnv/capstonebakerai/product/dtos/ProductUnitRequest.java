package com.manhnv.capstonebakerai.product.dtos;

import com.manhnv.capstonebakerai.product.entities.UnitStatus;
import java.math.BigDecimal;
import java.util.UUID;

public record ProductUnitRequest(UUID unitId, String unitName, BigDecimal conversionFactor,
        Boolean base, Boolean sale, BigDecimal sellingPrice, UnitStatus status) { }
