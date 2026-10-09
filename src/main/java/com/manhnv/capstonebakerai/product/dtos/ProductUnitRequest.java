package com.manhnv.capstonebakerai.product.dtos;

import com.manhnv.capstonebakerai.product.entities.UnitStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

public record ProductUnitRequest(UUID unitId,
        @NotBlank @Size(max = 100) String unitName,
        @NotNull @DecimalMin(value = "0.01") @Digits(integer = 8, fraction = 2) BigDecimal conversionFactor,
        @NotNull Boolean base,
        Boolean sale,
        @NotNull @DecimalMin("0.00") @Digits(integer = 13, fraction = 2) BigDecimal sellingPrice,
        UnitStatus status) { }
