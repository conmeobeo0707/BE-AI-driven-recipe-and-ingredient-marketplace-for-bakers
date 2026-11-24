package com.manhnv.capstonebakerai.report.dto.response;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

public record FinancialReportResponse(
        YearMonth from,
        YearMonth to,
        List<MonthlyFinancial> months,
        MonthlyFinancial total) {

    public record MonthlyFinancial(
            String month,                 // "2026-11" hoặc "TOTAL"
            long deliveredOrders,
            BigDecimal grossSales,        // (1) tổng tiền hàng
            BigDecimal discounts,         // (2) giảm giá tay + voucher
            BigDecimal netRevenue,        // (3) = (1) - (2)
            BigDecimal cogs,              // (4) giá vốn hàng bán
            BigDecimal grossProfit,       // (5) = (3) - (4)
            BigDecimal operatingExpenses, // (6) phiếu chi
            BigDecimal stocktakeLoss,     // (7) hụt kho khi kiểm kho
            BigDecimal disposalLoss,      // (8) hủy hàng
            BigDecimal totalShrinkage,    // (9) = (7) + (8)
            BigDecimal netProfit) {       // (10) = (5) - (6) - (9)
    }
}
