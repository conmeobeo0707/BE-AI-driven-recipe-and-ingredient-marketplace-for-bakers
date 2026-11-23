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
import com.manhnv.capstonebakerai.expense.enums.ExpenseCategoryType;
import com.manhnv.capstonebakerai.expense.enums.ExpenseVoucherStatus;
import com.manhnv.capstonebakerai.expense.mapper.ExpenseVoucherMapper;
import com.manhnv.capstonebakerai.expense.repository.ExpenseCategoryRepository;
import com.manhnv.capstonebakerai.expense.repository.ExpenseVoucherRepository;
import com.manhnv.capstonebakerai.user.service.UserSummaryService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ExpenseVoucherServiceImplTest {

    @Mock
    ExpenseVoucherRepository voucherRepository;
    @Mock
    ExpenseCategoryRepository categoryRepository;
    @Mock
    ExpenseVoucherMapper mapper;
    @Mock
    UserSummaryService userSummaryService;
    @InjectMocks
    ExpenseVoucherServiceImpl voucherService;

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    private void loginAs(String role) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "user", null, List.of(new SimpleGrantedAuthority(role))
                )
        );
    }

    // Create
    // UTCID01
    @Test
    void create_staffWithNonSalaryCategory_shouldSaveVoucher() {
        UUID userId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        ExpenseCategory category = ExpenseCategory.builder()
                .name("Electricity").type(ExpenseCategoryType.OTHER).build();
        CreateExpenseVoucherRequest request =
                new CreateExpenseVoucherRequest(categoryId, new BigDecimal("500000"), "Pay electricity bill");
        ExpenseVoucherResponse expected = mock(ExpenseVoucherResponse.class);
        loginAs("ROLE_STAFF");

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(voucherRepository.existsByCode(anyString())).thenReturn(false);
        when(voucherRepository.save(any(ExpenseVoucher.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mapper.toResponse(any(ExpenseVoucher.class))).thenReturn(expected);

        ExpenseVoucherResponse response = voucherService.create(userId, request);

        ArgumentCaptor<ExpenseVoucher> captor = ArgumentCaptor.forClass(ExpenseVoucher.class);
        verify(voucherRepository).save(captor.capture());
        ExpenseVoucher saved = captor.getValue();
        assertThat(response).isNotNull();
        assertThat(saved.getCode()).startsWith("PC-");
        assertThat(saved.getAmount()).isEqualByComparingTo("500000");
        assertThat(saved.getCreatedBy()).isEqualTo(userId);
        assertThat(saved.getExpenseCategory()).isSameAs(category);
    }

    // UTCID02
    @Test
    void create_managerWithSalaryCategory_shouldSaveVoucher() {
        UUID userId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        ExpenseCategory category = ExpenseCategory.builder()
                .name("Salary").type(ExpenseCategoryType.SALARY).build();
        CreateExpenseVoucherRequest request =
                new CreateExpenseVoucherRequest(categoryId, new BigDecimal("10000000"), "Monthly salary");
        loginAs("ROLE_MANAGER");

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(voucherRepository.existsByCode(anyString())).thenReturn(false);
        when(voucherRepository.save(any(ExpenseVoucher.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mapper.toResponse(any(ExpenseVoucher.class))).thenReturn(mock(ExpenseVoucherResponse.class));

        voucherService.create(userId, request);

        verify(voucherRepository).save(any(ExpenseVoucher.class));
    }

    // UTCID03
    @Test
    void create_codeCollision_shouldRegenerateCode() {
        UUID categoryId = UUID.randomUUID();
        ExpenseCategory category = ExpenseCategory.builder()
                .name("Rent").type(ExpenseCategoryType.OTHER).build();
        CreateExpenseVoucherRequest request =
                new CreateExpenseVoucherRequest(categoryId, new BigDecimal("100000"), "Rent");
        loginAs("ROLE_STAFF");

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(voucherRepository.existsByCode(anyString())).thenReturn(true, false);
        when(voucherRepository.save(any(ExpenseVoucher.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mapper.toResponse(any(ExpenseVoucher.class))).thenReturn(mock(ExpenseVoucherResponse.class));

        voucherService.create(UUID.randomUUID(), request);

        verify(voucherRepository, times(2)).existsByCode(anyString());
        verify(voucherRepository).save(any(ExpenseVoucher.class));
    }

    // UTCID04
    @Test
    void create_categoryNotFound_shouldThrow() {
        UUID categoryId = UUID.randomUUID();
        CreateExpenseVoucherRequest request =
                new CreateExpenseVoucherRequest(categoryId, new BigDecimal("100000"), "Rent");

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> voucherService.create(UUID.randomUUID(), request))
                .isInstanceOf(AppException.class);
        verify(voucherRepository, never()).save(any());
    }

    // UTCID05
    @Test
    void create_staffWithSalaryCategory_shouldThrowRequireManager() {
        UUID categoryId = UUID.randomUUID();
        ExpenseCategory category = ExpenseCategory.builder()
                .name("Salary").type(ExpenseCategoryType.SALARY).build();
        CreateExpenseVoucherRequest request =
                new CreateExpenseVoucherRequest(categoryId, new BigDecimal("10000000"), "Salary");
        loginAs("ROLE_STAFF");

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));

        assertThatThrownBy(() -> voucherService.create(UUID.randomUUID(), request))
                .isInstanceOf(AppException.class);
        verify(voucherRepository, never()).save(any());
    }

    // Update
    // UTCID01
    @Test
    void update_staffWithNonSalaryCategory_shouldUpdateFields() {
        UUID id = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        ExpenseVoucher voucher = ExpenseVoucher.builder()
                .status(ExpenseVoucherStatus.ACTIVE)
                .amount(new BigDecimal("100000"))
                .reason("Old reason")
                .build();
        ExpenseCategory newCategory = ExpenseCategory.builder()
                .name("Rent").type(ExpenseCategoryType.OTHER).build();
        UpdateExpenseVoucherRequest request =
                new UpdateExpenseVoucherRequest(categoryId, new BigDecimal("250000"), "New reason");
        loginAs("ROLE_STAFF");

        when(voucherRepository.findById(id)).thenReturn(Optional.of(voucher));
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(newCategory));
        when(voucherRepository.save(voucher)).thenReturn(voucher);
        when(mapper.toResponse(voucher)).thenReturn(mock(ExpenseVoucherResponse.class));

        voucherService.update(id, request);

        assertThat(voucher.getExpenseCategory()).isSameAs(newCategory);
        assertThat(voucher.getAmount()).isEqualByComparingTo("250000");
        assertThat(voucher.getReason()).isEqualTo("New reason");
        verify(voucherRepository).save(voucher);
    }

    // UTCID02
    @Test
    void update_managerWithSalaryCategory_shouldUpdateFields() {
        UUID id = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        ExpenseVoucher voucher = ExpenseVoucher.builder()
                .status(ExpenseVoucherStatus.ACTIVE)
                .amount(new BigDecimal("100000"))
                .reason("Old reason")
                .build();
        ExpenseCategory salaryCategory = ExpenseCategory.builder()
                .name("Salary").type(ExpenseCategoryType.SALARY).build();
        UpdateExpenseVoucherRequest request =
                new UpdateExpenseVoucherRequest(categoryId, new BigDecimal("10000000"), "Monthly salary");
        loginAs("ROLE_MANAGER");

        when(voucherRepository.findById(id)).thenReturn(Optional.of(voucher));
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(salaryCategory));
        when(voucherRepository.save(voucher)).thenReturn(voucher);
        when(mapper.toResponse(voucher)).thenReturn(mock(ExpenseVoucherResponse.class));

        voucherService.update(id, request);

        assertThat(voucher.getExpenseCategory()).isSameAs(salaryCategory);
        assertThat(voucher.getAmount()).isEqualByComparingTo("10000000");
        verify(voucherRepository).save(voucher);
    }

    // UTCID03
    @Test
    void update_voucherNotFound_shouldThrow() {
        UUID id = UUID.randomUUID();
        UpdateExpenseVoucherRequest request =
                new UpdateExpenseVoucherRequest(UUID.randomUUID(), new BigDecimal("1"), "x");

        when(voucherRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> voucherService.update(id, request))
                .isInstanceOf(AppException.class);
        verify(voucherRepository, never()).save(any());
    }

    // UTCID04
    @Test
    void update_cancelledVoucher_shouldThrow() {
        UUID id = UUID.randomUUID();
        ExpenseVoucher voucher = ExpenseVoucher.builder().status(ExpenseVoucherStatus.CANCELLED).build();
        UpdateExpenseVoucherRequest request =
                new UpdateExpenseVoucherRequest(UUID.randomUUID(), new BigDecimal("1"), "x");

        when(voucherRepository.findById(id)).thenReturn(Optional.of(voucher));

        assertThatThrownBy(() -> voucherService.update(id, request))
                .isInstanceOf(AppException.class);
        verify(categoryRepository, never()).findById(any());
        verify(voucherRepository, never()).save(any());
    }

    // UTCID05
    @Test
    void update_categoryNotFound_shouldThrow() {
        UUID id = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        ExpenseVoucher voucher = ExpenseVoucher.builder().status(ExpenseVoucherStatus.ACTIVE).build();
        UpdateExpenseVoucherRequest request =
                new UpdateExpenseVoucherRequest(categoryId, new BigDecimal("1"), "x");

        when(voucherRepository.findById(id)).thenReturn(Optional.of(voucher));
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> voucherService.update(id, request))
                .isInstanceOf(AppException.class);
        verify(voucherRepository, never()).save(any());
    }

    // UTCID06
    @Test
    void update_staffChangeToSalaryCategory_shouldThrowRequireManager() {
        UUID id = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        ExpenseVoucher voucher = ExpenseVoucher.builder()
                .status(ExpenseVoucherStatus.ACTIVE)
                .amount(new BigDecimal("100000"))
                .reason("Old reason")
                .build();
        ExpenseCategory salaryCategory = ExpenseCategory.builder()
                .name("Salary").type(ExpenseCategoryType.SALARY).build();
        UpdateExpenseVoucherRequest request =
                new UpdateExpenseVoucherRequest(categoryId, new BigDecimal("10000000"), "Salary");
        loginAs("ROLE_STAFF");

        when(voucherRepository.findById(id)).thenReturn(Optional.of(voucher));
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(salaryCategory));

        assertThatThrownBy(() -> voucherService.update(id, request))
                .isInstanceOf(AppException.class);
        verify(voucherRepository, never()).save(any());
        assertThat(voucher.getAmount()).isEqualByComparingTo("100000");
        assertThat(voucher.getReason()).isEqualTo("Old reason");
    }

    // Cancel
    // UTCID01
    @Test
    void cancel_activeVoucherWithReason_shouldCancel() {
        UUID id = UUID.randomUUID();
        ExpenseVoucher voucher = ExpenseVoucher.builder().status(ExpenseVoucherStatus.ACTIVE).build();
        CancelExpenseVoucherRequest request = new CancelExpenseVoucherRequest("Entered wrong amount");

        when(voucherRepository.findById(id)).thenReturn(Optional.of(voucher));
        when(voucherRepository.save(voucher)).thenReturn(voucher);
        when(mapper.toResponse(voucher)).thenReturn(mock(ExpenseVoucherResponse.class));

        voucherService.cancel(id, request);

        assertThat(voucher.getStatus()).isEqualTo(ExpenseVoucherStatus.CANCELLED);
        assertThat(voucher.getCancelReason()).isEqualTo("Entered wrong amount");
        assertThat(voucher.getCancelledAt()).isNotNull();
        verify(voucherRepository).save(voucher);
    }

    // UTCID02
    @Test
    void cancel_nullRequest_shouldCancelWithoutReason() {
        UUID id = UUID.randomUUID();
        ExpenseVoucher voucher = ExpenseVoucher.builder().status(ExpenseVoucherStatus.ACTIVE).build();

        when(voucherRepository.findById(id)).thenReturn(Optional.of(voucher));
        when(voucherRepository.save(voucher)).thenReturn(voucher);
        when(mapper.toResponse(voucher)).thenReturn(mock(ExpenseVoucherResponse.class));

        voucherService.cancel(id, null);

        assertThat(voucher.getStatus()).isEqualTo(ExpenseVoucherStatus.CANCELLED);
        assertThat(voucher.getCancelReason()).isNull();
        assertThat(voucher.getCancelledAt()).isNotNull();
    }

    // UTCID03
    @Test
    void cancel_voucherNotFound_shouldThrow() {
        UUID id = UUID.randomUUID();

        when(voucherRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> voucherService.cancel(id, new CancelExpenseVoucherRequest("x")))
                .isInstanceOf(AppException.class);
        verify(voucherRepository, never()).save(any());
    }

    // UTCID04
    @Test
    void cancel_alreadyCancelled_shouldThrow() {
        UUID id = UUID.randomUUID();
        ExpenseVoucher voucher = ExpenseVoucher.builder().status(ExpenseVoucherStatus.CANCELLED).build();

        when(voucherRepository.findById(id)).thenReturn(Optional.of(voucher));

        assertThatThrownBy(() -> voucherService.cancel(id, new CancelExpenseVoucherRequest("x")))
                .isInstanceOf(AppException.class);
        verify(voucherRepository, never()).save(any());
    }

    // Get all
    // UTCID01
    @Test
    void getAll_hasVouchers_shouldReturnMappedPage() {
        Pageable pageable = PageRequest.of(0, 10);
        ExpenseVoucher v1 = ExpenseVoucher.builder().build();
        ExpenseVoucher v2 = ExpenseVoucher.builder().build();
        ExpenseVoucherResponse r1 = mock(ExpenseVoucherResponse.class);
        ExpenseVoucherResponse r2 = mock(ExpenseVoucherResponse.class);

        when(voucherRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(v1, v2), pageable, 2));
        when(mapper.toResponse(v1)).thenReturn(r1);
        when(mapper.toResponse(v2)).thenReturn(r2);

        Page<ExpenseVoucherResponse> result = voucherService.getAll(pageable);

        assertThat(result.getContent()).containsExactly(r1, r2);
        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    // UTCID02
    @Test
    void getAll_noVouchers_shouldReturnEmptyPage() {
        Pageable pageable = PageRequest.of(0, 10);

        when(voucherRepository.findAll(pageable)).thenReturn(Page.empty(pageable));

        Page<ExpenseVoucherResponse> result = voucherService.getAll(pageable);

        assertThat(result.getContent()).isEmpty();
        verifyNoInteractions(mapper);
    }

    // Get by id
    // UTCID01
    @Test
    void getById_existingVoucher_shouldReturnResponse() {
        UUID id = UUID.randomUUID();
        ExpenseVoucher voucher = ExpenseVoucher.builder().build();
        ExpenseVoucherResponse expected = mock(ExpenseVoucherResponse.class);

        when(voucherRepository.findById(id)).thenReturn(Optional.of(voucher));
        when(mapper.toResponse(voucher)).thenReturn(expected);

        assertThat(voucherService.getById(id)).isNotNull();
    }

    // UTCID02
    @Test
    void getById_voucherNotFound_shouldThrow() {
        UUID id = UUID.randomUUID();

        when(voucherRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> voucherService.getById(id))
                .isInstanceOf(AppException.class);
        verifyNoInteractions(mapper);
    }

    // Get report
    // UTCID01
    @Test
    void getReport_withMonthAndData_shouldSumTotals() {
        YearMonth month = YearMonth.of(2026, 3);
        LocalDateTime from = LocalDateTime.of(2026, 3, 1, 0, 0);
        LocalDateTime to = LocalDateTime.of(2026, 4, 1, 0, 0);
        ExpenseCategorySummary s1 = mock(ExpenseCategorySummary.class);
        ExpenseCategorySummary s2 = mock(ExpenseCategorySummary.class);

        when(s1.totalAmount()).thenReturn(new BigDecimal("300000"));
        when(s1.voucherCount()).thenReturn(2L);
        when(s2.totalAmount()).thenReturn(new BigDecimal("200000"));
        when(s2.voucherCount()).thenReturn(3L);
        when(voucherRepository.summarizeByCategory(ExpenseVoucherStatus.ACTIVE, from, to, null))
                .thenReturn(List.of(s1, s2));

        ExpenseReportResponse response = voucherService.getReport(month, null);

        assertThat(response.month()).isEqualTo("2026-03");
        assertThat(response.totalAmount()).isEqualByComparingTo("500000");
        assertThat(response.totalVouchers()).isEqualTo(5L);
        assertThat(response.categories()).hasSize(2);
    }

    // UTCID02
    @Test
    void getReport_nullMonth_shouldUseCurrentMonth() {
        when(voucherRepository.summarizeByCategory(any(), any(), any(), any())).thenReturn(List.of());

        ExpenseReportResponse response = voucherService.getReport(null, null);

        assertThat(response.month()).isEqualTo(YearMonth.now().toString());
    }

    // UTCID03
    @Test
    void getReport_withExistingCategory_shouldFilterByCategory() {
        YearMonth month = YearMonth.of(2026, 3);
        UUID categoryId = UUID.randomUUID();
        LocalDateTime from = LocalDateTime.of(2026, 3, 1, 0, 0);
        LocalDateTime to = LocalDateTime.of(2026, 4, 1, 0, 0);

        when(categoryRepository.existsById(categoryId)).thenReturn(true);
        when(voucherRepository.summarizeByCategory(ExpenseVoucherStatus.ACTIVE, from, to, categoryId))
                .thenReturn(List.of());

        ExpenseReportResponse response = voucherService.getReport(month, categoryId);

        assertThat(response.categoryId()).isEqualTo(categoryId);
        verify(voucherRepository).summarizeByCategory(ExpenseVoucherStatus.ACTIVE, from, to, categoryId);
    }

    // UTCID04
    @Test
    void getReport_categoryNotFound_shouldThrow() {
        UUID categoryId = UUID.randomUUID();

        when(categoryRepository.existsById(categoryId)).thenReturn(false);

        assertThatThrownBy(() -> voucherService.getReport(YearMonth.of(2026, 3), categoryId))
                .isInstanceOf(AppException.class);
        verify(voucherRepository, never()).summarizeByCategory(any(), any(), any(), any());
    }

    // UTCID05 (Boundary: tháng 12)
    @Test
    void getReport_december_shouldUseJanuaryNextYearAsUpperBound() {
        YearMonth month = YearMonth.of(2026, 12);
        LocalDateTime from = LocalDateTime.of(2026, 12, 1, 0, 0);
        LocalDateTime to = LocalDateTime.of(2027, 1, 1, 0, 0);

        when(voucherRepository.summarizeByCategory(ExpenseVoucherStatus.ACTIVE, from, to, null))
                .thenReturn(List.of());

        ExpenseReportResponse response = voucherService.getReport(month, null);

        assertThat(response.month()).isEqualTo("2026-12");
        assertThat(response.totalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.totalVouchers()).isZero();
        assertThat(response.categories()).isEmpty();
    }

    // Get list voucher from report
    // UTCID05
    @Test
    void getListVoucherFromReport_categoryNotFound_shouldThrow() {
        UUID categoryId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 10);

        when(categoryRepository.existsById(categoryId)).thenReturn(false);

        assertThatThrownBy(() ->
                voucherService.getListVoucherFromReport(YearMonth.of(2026, 3), categoryId, pageable))
                .isInstanceOf(AppException.class);
        verifyNoInteractions(userSummaryService);
        verify(voucherRepository, never()).findVouchersByMonth(any(), any(), any(), any(), any());
    }
}
