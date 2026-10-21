package com.manhnv.capstonebakerai.cart.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record AddCartItemRequest(

        @Schema(
                description = "ID sản phẩm cần thêm",
                example = "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotNull UUID productId,
        @Schema(
                description = "ID đơn vị tính (UOM) của sản phẩm",
                example = "7c9e6679-7425-40de-944b-e07fc1f90ae7",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotNull UUID uomId,
        @Schema(
                description = "Số lượng sản phẩm, phải lớn hơn 0",
                example = "2.5",
                minimum = "0.001",
                maximum = "9999999999.999",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotNull @DecimalMin(value = "0", inclusive = false, message = "Quantity must greater than 0")
        @Digits(integer = 10, fraction = 3)
        BigDecimal quantity
        ) {
}
