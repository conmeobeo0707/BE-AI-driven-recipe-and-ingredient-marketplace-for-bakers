package com.manhnv.capstonebakerai.voucher.dto.request;

import com.manhnv.capstonebakerai.voucher.enums.DiscountType;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record UpdateVoucherRequest(
        @NotBlank @Size(max = 150) String name,
        @Size(max = 500) String description,
        @NotNull DiscountType discountType,
        @NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal discountValue,
        @DecimalMin(value = "0", inclusive = false) BigDecimal maxDiscountAmount,
        @DecimalMin("0") BigDecimal minOrderValue,
        @NotNull @Min(1) Integer totalQuantity,
        @NotNull @Min(1) Integer maxUsagePerCustomer,
        @NotNull LocalDateTime startDate,
        @NotNull LocalDateTime endDate
) {
}
