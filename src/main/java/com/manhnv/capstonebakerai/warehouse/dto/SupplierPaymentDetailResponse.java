package com.manhnv.capstonebakerai.warehouse.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record SupplierPaymentDetailResponse(SupplierPaymentResponse payment, List<Allocation> details) {
    public record Allocation(UUID id, UUID stockInNoteId, String stockInNoteCode, BigDecimal amount) { }
}
