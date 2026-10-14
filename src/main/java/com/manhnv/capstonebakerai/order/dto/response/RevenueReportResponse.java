package com.manhnv.capstonebakerai.order.dto.response;

import com.manhnv.capstonebakerai.order.enums.RevenuePeriodType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record RevenueReportResponse(
        RevenuePeriodType type,
        LocalDate from,
        LocalDate to,
        BigDecimal totalRevenue,
        long totalOrders,
        List<RevenuePoint> points
) {
    public record RevenuePoint(String period, BigDecimal revenue, long orderCount) {}
}
