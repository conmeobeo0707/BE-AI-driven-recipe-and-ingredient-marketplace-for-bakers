package com.manhnv.capstonebakerai.expense.service;

import com.manhnv.capstonebakerai.common.exception.AppException;
import com.manhnv.capstonebakerai.expense.dto.request.CancelExpenseVoucherRequest;
import com.manhnv.capstonebakerai.expense.dto.request.CreateExpenseVoucherRequest;
import com.manhnv.capstonebakerai.expense.dto.request.UpdateExpenseVoucherRequest;
import com.manhnv.capstonebakerai.expense.dto.response.ExpenseCategorySummary;
import com.manhnv.capstonebakerai.expense.dto.response.ExpenseReportResponse;
import com.manhnv.capstonebakerai.expense.dto.response.ExpenseVoucherResponse;
import com.manhnv.capstonebakerai.expense.entity.ExpenseCategory;
import com.manhnv.capstonebakerai.expense.entity.ExpenseVoucher;
import com.manhnv.capstonebakerai.expense.enums.ExpenseVoucherStatus;
import com.manhnv.capstonebakerai.expense.exception.ExpenseCategoryErrorCode;
import com.manhnv.capstonebakerai.expense.exception.ExpenseVoucherErrorCode;
import com.manhnv.capstonebakerai.expense.mapper.ExpenseVoucherMapper;
import com.manhnv.capstonebakerai.expense.repository.ExpenseCategoryRepository;
import com.manhnv.capstonebakerai.expense.repository.ExpenseVoucherRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ExpenseVoucherServiceImpl implements ExpenseVoucherService {

    ExpenseVoucherRepository voucherRepository;
    ExpenseCategoryRepository categoryRepository;
    ExpenseVoucherMapper mapper;

    @Override
    @Transactional
    public ExpenseVoucherResponse create(UUID userId, CreateExpenseVoucherRequest request) {
        ExpenseCategory category = findCategoryOrThrow(request.expenseCategoryId());

        ExpenseVoucher voucher = ExpenseVoucher.builder()
                .code(generateCode())
                .expenseCategory(category)
                .amount(request.amount())
                .reason(request.reason())
                .createdBy(userId)
                .build();

        return mapper.toResponse(voucherRepository.save(voucher));
    }

    @Override
    @Transactional
    public ExpenseVoucherResponse update(UUID id, UpdateExpenseVoucherRequest request) {
        ExpenseVoucher voucher = findOrThrow(id);
        if (voucher.getStatus() == ExpenseVoucherStatus.CANCELLED) {
            throw new AppException(ExpenseVoucherErrorCode.EXPENSE_VOUCHER_CANNOT_UPDATE);
        }

        voucher.setExpenseCategory(findCategoryOrThrow(request.expenseCategoryId()));
        voucher.setAmount(request.amount());
        voucher.setReason(request.reason());

        return mapper.toResponse(voucherRepository.save(voucher));
    }

    @Override
    @Transactional
    public ExpenseVoucherResponse cancel(UUID id, CancelExpenseVoucherRequest request) {
        ExpenseVoucher voucher = findOrThrow(id);
        if (voucher.getStatus() == ExpenseVoucherStatus.CANCELLED) {
            throw new AppException(ExpenseVoucherErrorCode.EXPENSE_VOUCHER_ALREADY_CANCELLED);
        }
        voucher.setStatus(ExpenseVoucherStatus.CANCELLED);
        voucher.setCancelReason(request.cancelReason());
        voucher.setCancelledAt(LocalDateTime.now());
        return mapper.toResponse(voucherRepository.save(voucher));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ExpenseVoucherResponse> getAll(Pageable pageable) {
        return voucherRepository.findAll(pageable).map(mapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public ExpenseVoucherResponse getById(UUID id) {
        return mapper.toResponse(findOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public ExpenseReportResponse getReport(YearMonth month, UUID categoryId) {
        YearMonth target = month != null ? month : YearMonth.now();

        if (categoryId != null && !categoryRepository.existsById(categoryId)) {
            throw new AppException(ExpenseCategoryErrorCode.EXPENSE_CATEGORY_NOT_FOUND);
        }

        LocalDateTime from = target.atDay(1).atStartOfDay();
        LocalDateTime to = target.plusMonths(1).atDay(1).atStartOfDay();

        List<ExpenseCategorySummary> categories = voucherRepository.summarizeByCategory(ExpenseVoucherStatus.ACTIVE, from, to, categoryId);

        BigDecimal total = categories.stream()
                .map(ExpenseCategorySummary::totalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long totalVouchers = categories.stream()
                .mapToLong(ExpenseCategorySummary::voucherCount)
                .sum();

        return new ExpenseReportResponse(target.toString(), categoryId, total, totalVouchers, categories);
    }

    @Override
    public Page<ExpenseVoucherResponse> getListVoucherFromReport(YearMonth month, UUID categoryId, Pageable pageable) {
        YearMonth target = month != null ? month : YearMonth.now();

        if (categoryId != null && !categoryRepository.existsById(categoryId)) {
            throw new AppException(ExpenseCategoryErrorCode.EXPENSE_CATEGORY_NOT_FOUND);
        }

        LocalDateTime from = target.atDay(1).atStartOfDay();
        LocalDateTime to = target.plusMonths(1).atDay(1).atStartOfDay();

        return voucherRepository.findVouchersByMonth(ExpenseVoucherStatus.ACTIVE, from, to, categoryId, pageable);
    }

    private String generateCode() {
        String code;
        do {
            code = "PC-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        } while (voucherRepository.existsByCode(code));
        return code;
    }

    private ExpenseVoucher findOrThrow(UUID id) {
        return voucherRepository.findById(id)
                .orElseThrow(() -> new AppException(ExpenseVoucherErrorCode.EXPENSE_VOUCHER_NOT_FOUND));
    }

    private ExpenseCategory findCategoryOrThrow(UUID id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new AppException(ExpenseCategoryErrorCode.EXPENSE_CATEGORY_NOT_FOUND));
    }
}
