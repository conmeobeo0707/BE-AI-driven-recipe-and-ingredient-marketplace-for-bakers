package com.manhnv.capstonebakerai.report.dto.response;

import java.math.BigDecimal;

public record RevenueMonthRow(Integer year, Integer month, Long orders,
                              BigDecimal gross, BigDecimal discounts, BigDecimal net) {
    public RevenueMonthRow {
        if (gross == null) gross = BigDecimal.ZERO;
        if (discounts == null) discounts = BigDecimal.ZERO;
        if (net == null) net = BigDecimal.ZERO;   // revenue có thể null
    }
    public String key() {
        return String.format("%d-%02d", year, month);
    }
}
