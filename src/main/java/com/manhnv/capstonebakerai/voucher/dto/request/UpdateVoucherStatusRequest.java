package com.manhnv.capstonebakerai.voucher.dto.request;

import com.manhnv.capstonebakerai.voucher.enums.VoucherStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateVoucherStatusRequest(@NotNull VoucherStatus status) {
}
