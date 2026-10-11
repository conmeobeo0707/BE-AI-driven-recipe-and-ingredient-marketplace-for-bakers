package com.manhnv.capstonebakerai.product.dtos;

import com.manhnv.capstonebakerai.product.entities.UnitStatus;
import java.math.BigDecimal;
import java.util.UUID;

public record ProductUnitResponse(UUID unitId, String unitName, BigDecimal conversionFactor,
        boolean base, boolean sale, BigDecimal sellingPrice, UnitStatus status) { }
