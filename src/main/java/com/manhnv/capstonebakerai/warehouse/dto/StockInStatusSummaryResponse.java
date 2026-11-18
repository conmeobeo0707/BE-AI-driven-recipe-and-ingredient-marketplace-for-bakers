package com.manhnv.capstonebakerai.warehouse.dto;

import java.math.BigDecimal;

public record StockInStatusSummaryResponse(
        long totalNotes,
        long pending,
        BigDecimal totalDebt,
        long cancelled
) {}
