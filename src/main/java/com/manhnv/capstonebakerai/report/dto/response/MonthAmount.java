package com.manhnv.capstonebakerai.report.dto.response;

import java.math.BigDecimal;

public record MonthAmount(Integer year, Integer month, BigDecimal amount) {
    public MonthAmount {
        if (amount == null) amount = BigDecimal.ZERO;
    }
    public String key() {
        return String.format("%d-%02d", year, month);
    }
}
