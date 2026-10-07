package com.manhnv.capstonebakerai.expense.dto.request;

import jakarta.validation.constraints.Size;

public record CancelExpenseVoucherRequest(
        @Size(max = 500) String cancelReason
) {
}
