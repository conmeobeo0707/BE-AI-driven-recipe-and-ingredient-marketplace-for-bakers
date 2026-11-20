package com.manhnv.capstonebakerai.report.service;

import com.manhnv.capstonebakerai.report.dto.response.FinancialReportResponse;

import java.time.YearMonth;

public interface FinancialReportService {
    FinancialReportResponse getMonthlyReport(YearMonth from, YearMonth to);
}
