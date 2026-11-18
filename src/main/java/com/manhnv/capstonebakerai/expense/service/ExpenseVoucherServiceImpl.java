package com.manhnv.capstonebakerai.expense.service;

import com.manhnv.capstonebakerai.common.exception.AppException;
import com.manhnv.capstonebakerai.expense.dto.request.CancelExpenseVoucherRequest;
import com.manhnv.capstonebakerai.expense.dto.request.CreateExpenseVoucherRequest;
import com.manhnv.capstonebakerai.expense.dto.request.UpdateExpenseVoucherRequest;
import com.manhnv.capstonebakerai.expense.dto.response.ExpenseCategorySummary;
import com.manhnv.capstonebakerai.expense.dto.response.ExpenseReportResponse;
import com.manhnv.capstonebakerai.expense.dto.response.ExpenseVoucherResponse;
import com.manhnv.capstonebakerai.expense.dto.response.ExpenseVoucherRow;
import com.manhnv.capstonebakerai.expense.dto.response.ExpenseVoucherSummaryResponse;
import com.manhnv.capstonebakerai.expense.dto.response.ExpenseVoucherWithNameResponse;
import com.manhnv.capstonebakerai.expense.entity.ExpenseCategory;
import com.manhnv.capstonebakerai.expense.entity.ExpenseVoucher;
import com.manhnv.capstonebakerai.expense.enums.ExpenseCategoryType;
import com.manhnv.capstonebakerai.expense.enums.ExpenseVoucherStatus;
import com.manhnv.capstonebakerai.expense.exception.ExpenseCategoryErrorCode;
import com.manhnv.capstonebakerai.expense.exception.ExpenseVoucherErrorCode;
import com.manhnv.capstonebakerai.expense.mapper.ExpenseVoucherMapper;
import com.manhnv.capstonebakerai.expense.repository.ExpenseCategoryRepository;
import com.manhnv.capstonebakerai.expense.repository.ExpenseVoucherRepository;
import com.manhnv.capstonebakerai.user.dto.UserSummaryResponse;
import com.manhnv.capstonebakerai.user.service.UserSummaryService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ExpenseVoucherServiceImpl implements ExpenseVoucherService {

    ExpenseVoucherRepository voucherRepository;
    ExpenseCategoryRepository categoryRepository;
    ExpenseVoucherMapper mapper;
    UserSummaryService userSummaryService;

    @Override
    @Transactional
    public ExpenseVoucherResponse create(UUID userId, CreateExpenseVoucherRequest request) {
        ExpenseCategory category = findCategoryOrThrow(request.expenseCategoryId());

        if (!isManager() && category.getType() == ExpenseCategoryType.SALARY) {
            throw new AppException(ExpenseVoucherErrorCode.REQUIRE_MANAGER_ROLE);
        }

        ExpenseVoucher voucher = ExpenseVoucher.builder()
                .code(generateCode())
                .expenseCategory(category)
                .amount(request.amount())
                .reason(request.reason())
                .createdBy(userId)
                .build();

        return enrich(voucherRepository.save(voucher));
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

        return enrich(voucherRepository.save(voucher));
    }

    @Override
    @Transactional
    public ExpenseVoucherResponse cancel(UUID id, CancelExpenseVoucherRequest request) {
        ExpenseVoucher voucher = findOrThrow(id);
        if (voucher.getStatus() == ExpenseVoucherStatus.CANCELLED) {
            throw new AppException(ExpenseVoucherErrorCode.EXPENSE_VOUCHER_ALREADY_CANCELLED);
        }
        voucher.setStatus(ExpenseVoucherStatus.CANCELLED);
        voucher.setCancelReason(request != null ? request.cancelReason() : null);
        voucher.setCancelledAt(LocalDateTime.now());
        return enrich(voucherRepository.save(voucher));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ExpenseVoucherResponse> getAll(Pageable pageable) {
        return enrichPage(voucherRepository.findAll(pageable));
    }

    @Override
    @Transactional(readOnly = true)
    public ExpenseVoucherResponse getById(UUID id) {
        return enrich(findOrThrow(id));
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
    public Page<ExpenseVoucherWithNameResponse> getListVoucherFromReport(YearMonth month, UUID categoryId, Pageable pageable) {
        YearMonth target = month != null ? month : YearMonth.now();

        if (categoryId != null && !categoryRepository.existsById(categoryId)) {
            throw new AppException(ExpenseCategoryErrorCode.EXPENSE_CATEGORY_NOT_FOUND);
        }

        LocalDateTime from = target.atDay(1).atStartOfDay();
        LocalDateTime to = target.plusMonths(1).atDay(1).atStartOfDay();

        // Unlike getReport() (active-only totals), this backs the voucher management table, which
        // needs both ACTIVE and CANCELLED rows so the UI's status tabs have something to filter.
        Page<ExpenseVoucherRow> rows = voucherRepository.findVouchersByMonth(null, from, to, categoryId, pageable);
        Map<UUID, UserSummaryResponse> creators = userSummaryService.getSummaries(
                rows.getContent().stream().map(ExpenseVoucherRow::createdBy).filter(Objects::nonNull).distinct().toList());
        return rows.map(row -> {
            UserSummaryResponse creator = resolveCreator(creators, row.createdBy());
            ExpenseVoucherResponse response = new ExpenseVoucherResponse(row.id(), row.code(), row.expenseCategoryId(), row.expenseCategoryName(),
                    row.amount(), row.reason(), row.status(), creator, row.createdAt());
            return new ExpenseVoucherWithNameResponse(response, creator != null ? creator.fullName() : null);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public ExpenseVoucherSummaryResponse getSummary(LocalDate from, LocalDate to) {
        LocalDateTime fromDateTime = from.atStartOfDay();
        LocalDateTime toDateTime = to.atStartOfDay();

        BigDecimal totalAmount = voucherRepository.sumAmountByStatusAndCreatedAtBetween(
                ExpenseVoucherStatus.ACTIVE, fromDateTime, toDateTime);
        long voucherCount = voucherRepository.countByStatusAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                ExpenseVoucherStatus.ACTIVE, fromDateTime, toDateTime);
        long cancelledCount = voucherRepository.countByStatusAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                ExpenseVoucherStatus.CANCELLED, fromDateTime, toDateTime);
        BigDecimal averagePerVoucher = voucherCount == 0 ? BigDecimal.ZERO
                : totalAmount.divide(BigDecimal.valueOf(voucherCount), 2, RoundingMode.HALF_UP);

        return new ExpenseVoucherSummaryResponse(totalAmount, voucherCount, cancelledCount, averagePerVoucher);
    }

    /** Fills in the mapper-skipped `createdBy` with a resolved name/avatar for a single entity. */
    private ExpenseVoucherResponse enrich(ExpenseVoucher entity) {
        ExpenseVoucherResponse base = mapper.toResponse(entity);
        UserSummaryResponse creator = userSummaryService.getSummary(entity.getCreatedBy());
        return new ExpenseVoucherResponse(base.id(), base.code(), base.expenseCategoryId(), base.expenseCategoryName(),
                base.amount(), base.reason(), base.status(), creator, base.createdAt());
    }

    /** Same as {@link #enrich(ExpenseVoucher)} but batches the user lookup for a whole page. */
    private Page<ExpenseVoucherResponse> enrichPage(Page<ExpenseVoucher> page) {
        Map<UUID, UserSummaryResponse> creators = userSummaryService.getSummaries(
                page.getContent().stream().map(ExpenseVoucher::getCreatedBy).filter(Objects::nonNull).distinct().toList());
        return page.map(entity -> {
            ExpenseVoucherResponse base = mapper.toResponse(entity);
            return new ExpenseVoucherResponse(base.id(), base.code(), base.expenseCategoryId(), base.expenseCategoryName(),
                    base.amount(), base.reason(), base.status(), resolveCreator(creators, entity.getCreatedBy()), base.createdAt());
        });
    }

    private UserSummaryResponse resolveCreator(Map<UUID, UserSummaryResponse> creators, UUID userId) {
        if (userId == null) return null;
        return creators.getOrDefault(userId, new UserSummaryResponse(userId, null, null));
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

    private boolean isManager() {
        return SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_MANAGER"));
    }
}
