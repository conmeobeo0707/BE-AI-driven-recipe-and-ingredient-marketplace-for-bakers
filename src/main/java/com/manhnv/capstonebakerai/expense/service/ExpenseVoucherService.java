package com.manhnv.capstonebakerai.expense.service;

import com.manhnv.capstonebakerai.expense.dto.request.CancelExpenseVoucherRequest;
import com.manhnv.capstonebakerai.expense.dto.request.CreateExpenseVoucherRequest;
import com.manhnv.capstonebakerai.expense.dto.request.UpdateExpenseVoucherRequest;
import com.manhnv.capstonebakerai.expense.dto.response.ExpenseReportResponse;
import com.manhnv.capstonebakerai.expense.dto.response.ExpenseVoucherResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.YearMonth;
import java.util.UUID;

public interface ExpenseVoucherService {
    ExpenseVoucherResponse create(UUID userId, CreateExpenseVoucherRequest request);
    ExpenseVoucherResponse update(UUID id, UpdateExpenseVoucherRequest request);
    ExpenseVoucherResponse cancel(UUID id, CancelExpenseVoucherRequest request);
    Page<ExpenseVoucherResponse> getAll(Pageable pageable);
    ExpenseVoucherResponse getById(UUID id);

    ExpenseReportResponse getReport(YearMonth month, UUID categoryId);
    Page<ExpenseVoucherResponse> getListVoucherFromReport(YearMonth month, UUID categoryId, Pageable pageable);
}
