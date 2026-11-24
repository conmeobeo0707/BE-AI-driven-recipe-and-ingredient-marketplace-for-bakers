package com.manhnv.capstonebakerai.report.service;

import com.manhnv.capstonebakerai.common.exception.AppException;
import com.manhnv.capstonebakerai.expense.enums.ExpenseVoucherStatus;
import com.manhnv.capstonebakerai.expense.repository.ExpenseVoucherRepository;
import com.manhnv.capstonebakerai.order.enums.OrderStatus;
import com.manhnv.capstonebakerai.order.exception.OrderErrorCode;
import com.manhnv.capstonebakerai.order.repository.OrderRepository;
import com.manhnv.capstonebakerai.report.dto.response.FinancialReportResponse;
import com.manhnv.capstonebakerai.report.dto.response.MonthAmount;
import com.manhnv.capstonebakerai.report.dto.response.RevenueMonthRow;
import com.manhnv.capstonebakerai.warehouse.enums.NoteStatus;
import com.manhnv.capstonebakerai.warehouse.enums.StockOutType;
import com.manhnv.capstonebakerai.warehouse.enums.StocktakeStatus;
import com.manhnv.capstonebakerai.warehouse.repository.StockOutNoteRepository;
import com.manhnv.capstonebakerai.warehouse.repository.StocktakeItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FinancialReportServiceImpl implements FinancialReportService{

    private static final int MAX_MONTHS = 36;

    private final OrderRepository orderRepository;
    private final StockOutNoteRepository stockOutNoteRepository;
    private final StocktakeItemRepository stocktakeItemRepository;
    private final ExpenseVoucherRepository expenseVoucherRepository;
    @Override
    @Transactional(readOnly = true)
    public FinancialReportResponse getMonthlyReport(YearMonth from, YearMonth to) {
        if (from.isAfter(to)) throw new AppException(OrderErrorCode.INVALID_DATE_RANGE);
        if (from.until(to, java.time.temporal.ChronoUnit.MONTHS) >= MAX_MONTHS) {
            throw new IllegalArgumentException("Tối đa " + MAX_MONTHS + " tháng");
        }

        LocalDateTime start = from.atDay(1).atStartOfDay();
        LocalDateTime end = to.plusMonths(1).atDay(1).atStartOfDay();

        Map<String, RevenueMonthRow> revenue = orderRepository
                .revenueByMonth(OrderStatus.DELIVERED, start, end).stream()
                .collect(Collectors.toMap(RevenueMonthRow::key, Function.identity()));

        Map<String, BigDecimal> cogs = toMap(stockOutNoteRepository.cogsByMonth(
                StockOutType.SALE, NoteStatus.COMPLETED, OrderStatus.DELIVERED, start, end));

        Map<String, BigDecimal> disposal = toMap(stockOutNoteRepository.disposalByMonth(
                StockOutType.DISPOSAL, NoteStatus.COMPLETED, start, end));

        Map<String, BigDecimal> stocktakeLoss = toMap(stocktakeItemRepository.stocktakeLossByMonth(
                StocktakeStatus.COMPLETED, start, end))
                .entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().negate())); // âm -> dương

        Map<String, BigDecimal> expenses = toMap(expenseVoucherRepository.expensesByMonth(
                ExpenseVoucherStatus.ACTIVE, start, end));

        List<FinancialReportResponse.MonthlyFinancial> months = new ArrayList<>();
        for (YearMonth ym = from; !ym.isAfter(to); ym = ym.plusMonths(1)) {
            String key = ym.toString();
            RevenueMonthRow r = revenue.get(key);
            months.add(build(key,
                    r == null ? 0 : r.orders(),
                    r == null ? BigDecimal.ZERO : r.gross(),
                    r == null ? BigDecimal.ZERO : r.discounts(),
                    r == null ? BigDecimal.ZERO : r.net(),
                    cogs.getOrDefault(key, BigDecimal.ZERO),
                    expenses.getOrDefault(key, BigDecimal.ZERO),
                    stocktakeLoss.getOrDefault(key, BigDecimal.ZERO),
                    disposal.getOrDefault(key, BigDecimal.ZERO)));
        }

        FinancialReportResponse.MonthlyFinancial total = build("TOTAL",
                months.stream().mapToLong(FinancialReportResponse.MonthlyFinancial::deliveredOrders).sum(),
                sum(months, FinancialReportResponse.MonthlyFinancial::grossSales),
                sum(months, FinancialReportResponse.MonthlyFinancial::discounts),
                sum(months, FinancialReportResponse.MonthlyFinancial::netRevenue),
                sum(months, FinancialReportResponse.MonthlyFinancial::cogs),
                sum(months, FinancialReportResponse.MonthlyFinancial::operatingExpenses),
                sum(months, FinancialReportResponse.MonthlyFinancial::stocktakeLoss),
                sum(months, FinancialReportResponse.MonthlyFinancial::disposalLoss));

        return new FinancialReportResponse(from, to, months, total);
    }

    private Map<String, BigDecimal> toMap(List<MonthAmount> rows) {
        return rows.stream().collect(Collectors.toMap(MonthAmount::key, MonthAmount::amount));
    }

    private FinancialReportResponse.MonthlyFinancial build(String month, long orders, BigDecimal gross, BigDecimal discounts, BigDecimal netRevenue, BigDecimal cogs, BigDecimal expenses, BigDecimal stocktakeLoss, BigDecimal disposalLoss) {
        BigDecimal grossProfit = netRevenue.subtract(cogs);
        BigDecimal shrinkage = stocktakeLoss.add(disposalLoss);
        BigDecimal netProfit = grossProfit.subtract(expenses).subtract(shrinkage);
        return new FinancialReportResponse.MonthlyFinancial(month, orders, scale(gross), scale(discounts), scale(netRevenue),
                scale(cogs), scale(grossProfit), scale(expenses), scale(stocktakeLoss),
                scale(disposalLoss), scale(shrinkage), scale(netProfit));
    }

    private BigDecimal sum(List<FinancialReportResponse.MonthlyFinancial> list, java.util.function.Function<FinancialReportResponse.MonthlyFinancial, BigDecimal> f) {
        return list.stream().map(f).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal scale(BigDecimal v) {
        return v.setScale(2, RoundingMode.HALF_UP);
    }
}
