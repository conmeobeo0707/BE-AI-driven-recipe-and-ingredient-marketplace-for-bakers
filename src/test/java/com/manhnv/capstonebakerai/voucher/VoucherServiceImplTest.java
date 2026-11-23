package com.manhnv.capstonebakerai.voucher;

import com.manhnv.capstonebakerai.common.exception.AppException;
import com.manhnv.capstonebakerai.voucher.dto.request.CreateVoucherRequest;
import com.manhnv.capstonebakerai.voucher.dto.request.UpdateVoucherRequest;
import com.manhnv.capstonebakerai.voucher.dto.response.VoucherPreviewResponse;
import com.manhnv.capstonebakerai.voucher.dto.response.VoucherResponse;
import com.manhnv.capstonebakerai.voucher.dto.response.VoucherSummaryResponse;
import com.manhnv.capstonebakerai.voucher.dto.response.VoucherUsageResponse;
import com.manhnv.capstonebakerai.voucher.entity.Voucher;
import com.manhnv.capstonebakerai.voucher.entity.VoucherUsage;
import com.manhnv.capstonebakerai.voucher.enums.DiscountType;
import com.manhnv.capstonebakerai.voucher.enums.VoucherDisplayStatus;
import com.manhnv.capstonebakerai.voucher.enums.VoucherStatus;
import com.manhnv.capstonebakerai.voucher.enums.VoucherUsageStatus;
import com.manhnv.capstonebakerai.voucher.exception.VoucherErrorCode;
import com.manhnv.capstonebakerai.voucher.mapper.VoucherMapper;
import com.manhnv.capstonebakerai.voucher.repository.VoucherRepository;
import com.manhnv.capstonebakerai.voucher.repository.VoucherUsageRepository;
import com.manhnv.capstonebakerai.voucher.service.VoucherServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class VoucherServiceImplTest {

    @Mock
    VoucherRepository voucherRepository;
    @Mock
    VoucherUsageRepository usageRepository;
    @Mock
    VoucherMapper mapper;
    @InjectMocks
    VoucherServiceImpl service;

    @Captor
    ArgumentCaptor<Voucher> voucherCaptor;
    @Captor
    ArgumentCaptor<VoucherUsage> usageCaptor;

    private final UUID managerId = UUID.randomUUID();
    private final UUID voucherId = UUID.randomUUID();
    private final UUID customerId = UUID.randomUUID();
    private final UUID orderId = UUID.randomUUID();

    // calculate() lấy LocalDateTime.now() bên trong, nên dùng mốc rất xa để test không bị flaky
    private static final LocalDateTime PAST = LocalDateTime.of(2020, 1, 1, 0, 0);
    private static final LocalDateTime FAR_FUTURE = LocalDateTime.of(2099, 12, 31, 23, 59);

    /** Voucher ACTIVE, đang trong thời hạn, còn số lượng (total = 100). */
    private Voucher runningVoucher(int usedQuantity) {
        Voucher v = existingVoucher(usedQuantity);
        v.setStartDate(PAST);
        v.setEndDate(FAR_FUTURE);
        return v;
    }

    private void stubUsedByCustomer(long count) {
        when(usageRepository.countByVoucherIdAndCustomerIdAndStatus(
                voucherId, customerId, VoucherUsageStatus.USED)).thenReturn(count);
    }

    // Ngày cố định ở tương lai xa, không phụ thuộc LocalDateTime.now()
    private static final LocalDateTime START = LocalDateTime.of(2030, 1, 1, 0, 0);
    private static final LocalDateTime END = LocalDateTime.of(2030, 12, 31, 23, 59);

    private static BigDecimal bd(String s) {
        return new BigDecimal(s);
    }

    /** Thứ tự tham số đoán từ VoucherServiceImpl.create. */
    private CreateVoucherRequest createRequest(String code, DiscountType type, BigDecimal value,
                                               BigDecimal maxDiscount, BigDecimal minOrder,
                                               LocalDateTime start, LocalDateTime end) {
        return new CreateVoucherRequest(code, "  Summer Sale  ", "desc", type, value,
                maxDiscount, minOrder, 100, 1, start, end);
    }

    /** Thứ tự tham số đoán từ VoucherServiceImpl.update (không có code). */
    private UpdateVoucherRequest updateRequest(DiscountType type, BigDecimal value, BigDecimal maxDiscount,
                                               BigDecimal minOrder, int totalQuantity, int maxUsagePerCustomer,
                                               LocalDateTime start, LocalDateTime end) {
        return new UpdateVoucherRequest("  New Name  ", "new desc", type, value, maxDiscount,
                minOrder, totalQuantity, maxUsagePerCustomer, start, end);
    }

    /** Request giữ nguyên các trường bị khóa của existingVoucher, chỉ đổi totalQuantity. */
    private UpdateVoucherRequest lockedSameRequest(int totalQuantity) {
        return updateRequest(DiscountType.PERCENT, bd("10"), bd("50000"), bd("100000"),
                totalQuantity, 1, START, END);
    }

    private Voucher existingVoucher(int usedQuantity) {
        return Voucher.builder()
                .id(voucherId)
                .code("SALE10")
                .name("Old Name")
                .description("old desc")
                .discountType(DiscountType.PERCENT)
                .discountValue(bd("10"))
                .maxDiscountAmount(bd("50000"))
                .minOrderValue(bd("100000"))
                .totalQuantity(100)
                .usedQuantity(usedQuantity)
                .maxUsagePerCustomer(1)
                .startDate(START)
                .endDate(END)
                .createdBy(managerId)
                .build();
    }

    private VoucherUsage usageOf(Voucher voucher) {
        return VoucherUsage.builder()
                .id(UUID.randomUUID())
                .voucher(voucher)
                .customerId(UUID.randomUUID())
                .orderId(UUID.randomUUID())
                .discountAmount(bd("10000"))
                .usedAt(LocalDateTime.of(2030, 2, 1, 10, 0))
                .build();
    }

    private void stubSaveReturnsArgument() {
        when(voucherRepository.save(any(Voucher.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    // Create
    // UTCID01
    @Test
    void create_withCode_shouldNormalizeCodeAndSaveVoucher() {
        VoucherResponse response = mock(VoucherResponse.class);
        when(voucherRepository.existsByCode("SALE10")).thenReturn(false);
        stubSaveReturnsArgument();
        when(mapper.toResponse(any(Voucher.class))).thenReturn(response);

        VoucherResponse result = service.create(managerId,
                createRequest("  sale10 ", DiscountType.PERCENT, bd("10"), bd("50000"), bd("100000"), START, END));

        assertThat(result).isSameAs(response);
        verify(voucherRepository).save(voucherCaptor.capture());
        Voucher saved = voucherCaptor.getValue();
        assertThat(saved.getCode()).isEqualTo("SALE10");
        assertThat(saved.getName()).isEqualTo("Summer Sale");
        assertThat(saved.getDiscountType()).isEqualTo(DiscountType.PERCENT);
        assertThat(saved.getDiscountValue()).isEqualByComparingTo("10");
        assertThat(saved.getMaxDiscountAmount()).isEqualByComparingTo("50000");
        assertThat(saved.getMinOrderValue()).isEqualByComparingTo("100000");
        assertThat(saved.getTotalQuantity()).isEqualTo(100);
        assertThat(saved.getMaxUsagePerCustomer()).isEqualTo(1);
        assertThat(saved.getCreatedBy()).isEqualTo(managerId);
    }

    // UTCID02
    @Test
    void create_codeNull_shouldGenerateCode() {
        VoucherResponse response = mock(VoucherResponse.class);
        when(voucherRepository.existsByCode(anyString())).thenReturn(false);
        stubSaveReturnsArgument();
        when(mapper.toResponse(any(Voucher.class))).thenReturn(response);

        VoucherResponse result = service.create(managerId,
                createRequest(null, DiscountType.PERCENT, bd("10"), bd("50000"), bd("0"), START, END));

        assertThat(result).isSameAs(response);
        verify(voucherRepository).save(voucherCaptor.capture());
        assertThat(voucherCaptor.getValue().getCode()).matches("VC[A-HJ-NP-Z2-9]{8}");
    }

    // UTCID03
    @Test
    void create_codeBlank_shouldGenerateCode() {
        VoucherResponse response = mock(VoucherResponse.class);
        when(voucherRepository.existsByCode(anyString())).thenReturn(false);
        stubSaveReturnsArgument();
        when(mapper.toResponse(any(Voucher.class))).thenReturn(response);

        service.create(managerId,
                createRequest("   ", DiscountType.PERCENT, bd("10"), bd("50000"), bd("0"), START, END));

        verify(voucherRepository).save(voucherCaptor.capture());
        assertThat(voucherCaptor.getValue().getCode()).matches("VC[A-HJ-NP-Z2-9]{8}");
    }

    // UTCID04
    @Test
    void create_generatedCodeCollides_shouldGenerateAgain() {
        VoucherResponse response = mock(VoucherResponse.class);
        when(voucherRepository.existsByCode(anyString())).thenReturn(true, false);
        stubSaveReturnsArgument();
        when(mapper.toResponse(any(Voucher.class))).thenReturn(response);

        service.create(managerId,
                createRequest(null, DiscountType.PERCENT, bd("10"), bd("50000"), bd("0"), START, END));

        verify(voucherRepository, times(2)).existsByCode(anyString());
        verify(voucherRepository).save(any(Voucher.class));
    }

    // UTCID05
    @Test
    void create_fixedAmount_shouldDropMaxDiscountAndDefaultMinOrderToZero() {
        VoucherResponse response = mock(VoucherResponse.class);
        when(voucherRepository.existsByCode("FIX50K")).thenReturn(false);
        stubSaveReturnsArgument();
        when(mapper.toResponse(any(Voucher.class))).thenReturn(response);

        service.create(managerId,
                createRequest("FIX50K", DiscountType.FIXED_AMOUNT, bd("50000"), bd("99999"), null, START, END));

        verify(voucherRepository).save(voucherCaptor.capture());
        Voucher saved = voucherCaptor.getValue();
        assertThat(saved.getDiscountType()).isEqualTo(DiscountType.FIXED_AMOUNT);
        assertThat(saved.getMaxDiscountAmount()).isNull();
        assertThat(saved.getMinOrderValue()).isEqualByComparingTo("0");
    }

    // UTCID06
    @Test
    void create_codeExists_shouldThrowCodeExisted() {
        when(voucherRepository.existsByCode("SALE10")).thenReturn(true);

        assertThatThrownBy(() -> service.create(managerId,
                createRequest("sale10", DiscountType.PERCENT, bd("10"), bd("50000"), bd("0"), START, END)))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(VoucherErrorCode.VOUCHER_CODE_EXISTED);

        verify(voucherRepository, never()).save(any());
    }

    // UTCID07
    @Test
    void create_endEqualsStart_shouldThrowInvalidDateRange() {
        when(voucherRepository.existsByCode("SALE10")).thenReturn(false);

        assertThatThrownBy(() -> service.create(managerId,
                createRequest("SALE10", DiscountType.PERCENT, bd("10"), bd("50000"), bd("0"), START, START)))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(VoucherErrorCode.VOUCHER_INVALID_DATE_RANGE);

        verify(voucherRepository, never()).save(any());
    }

    // UTCID08
    @Test
    void create_percentOver100_shouldThrowInvalidDiscountValue() {
        when(voucherRepository.existsByCode("SALE10")).thenReturn(false);

        assertThatThrownBy(() -> service.create(managerId,
                createRequest("SALE10", DiscountType.PERCENT, bd("101"), bd("50000"), bd("0"), START, END)))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(VoucherErrorCode.VOUCHER_INVALID_DISCOUNT_VALUE);

        verify(voucherRepository, never()).save(any());
    }

    // UTCID09
    @Test
    void create_percentEquals100_shouldSucceed() {
        VoucherResponse response = mock(VoucherResponse.class);
        when(voucherRepository.existsByCode("FULL100")).thenReturn(false);
        stubSaveReturnsArgument();
        when(mapper.toResponse(any(Voucher.class))).thenReturn(response);

        VoucherResponse result = service.create(managerId,
                createRequest("FULL100", DiscountType.PERCENT, bd("100"), bd("50000"), bd("0"), START, END));

        assertThat(result).isSameAs(response);
        verify(voucherRepository).save(voucherCaptor.capture());
        assertThat(voucherCaptor.getValue().getDiscountValue()).isEqualByComparingTo("100");
    }

    // Update
    // UTCID01
    @Test
    void update_noUsage_shouldUpdateAllFields() {
        Voucher voucher = existingVoucher(0);
        VoucherResponse response = mock(VoucherResponse.class);
        LocalDateTime newStart = START.plusDays(1);
        LocalDateTime newEnd = END.plusDays(1);
        when(voucherRepository.findById(voucherId)).thenReturn(Optional.of(voucher));
        when(usageRepository.existsByVoucherId(voucherId)).thenReturn(false);
        stubSaveReturnsArgument();
        when(mapper.toResponse(any(Voucher.class))).thenReturn(response);

        VoucherResponse result = service.update(voucherId,
                updateRequest(DiscountType.FIXED_AMOUNT, bd("20000"), bd("99999"), bd("50000"), 200, 3, newStart, newEnd));

        assertThat(result).isSameAs(response);
        verify(voucherRepository).save(voucherCaptor.capture());
        Voucher saved = voucherCaptor.getValue();
        assertThat(saved.getName()).isEqualTo("New Name");
        assertThat(saved.getDescription()).isEqualTo("new desc");
        assertThat(saved.getDiscountType()).isEqualTo(DiscountType.FIXED_AMOUNT);
        assertThat(saved.getDiscountValue()).isEqualByComparingTo("20000");
        assertThat(saved.getMaxDiscountAmount()).isNull();
        assertThat(saved.getMinOrderValue()).isEqualByComparingTo("50000");
        assertThat(saved.getTotalQuantity()).isEqualTo(200);
        assertThat(saved.getMaxUsagePerCustomer()).isEqualTo(3);
        assertThat(saved.getStartDate()).isEqualTo(newStart);
        assertThat(saved.getEndDate()).isEqualTo(newEnd);
    }

    // UTCID02
    @Test
    void update_hasUsageButOnlyUnlockedFieldsChanged_shouldUpdate() {
        Voucher voucher = existingVoucher(5);
        VoucherResponse response = mock(VoucherResponse.class);
        LocalDateTime newEnd = END.plusDays(30);
        when(voucherRepository.findById(voucherId)).thenReturn(Optional.of(voucher));
        stubSaveReturnsArgument();
        when(mapper.toResponse(any(Voucher.class))).thenReturn(response);

        VoucherResponse result = service.update(voucherId,
                updateRequest(DiscountType.PERCENT, bd("10"), bd("50000"), bd("100000"), 200, 1, START, newEnd));

        assertThat(result).isSameAs(response);
        verify(usageRepository, never()).existsByVoucherId(any());   // usedQuantity > 0 nên short-circuit
        verify(voucherRepository).save(voucherCaptor.capture());
        Voucher saved = voucherCaptor.getValue();
        assertThat(saved.getName()).isEqualTo("New Name");
        assertThat(saved.getTotalQuantity()).isEqualTo(200);
        assertThat(saved.getEndDate()).isEqualTo(newEnd);
        assertThat(saved.getDiscountValue()).isEqualByComparingTo("10");   // trường khóa giữ nguyên
    }

    // UTCID03
    @Test
    void update_totalQuantityEqualsUsed_shouldUpdate() {
        Voucher voucher = existingVoucher(5);
        VoucherResponse response = mock(VoucherResponse.class);
        when(voucherRepository.findById(voucherId)).thenReturn(Optional.of(voucher));
        stubSaveReturnsArgument();
        when(mapper.toResponse(any(Voucher.class))).thenReturn(response);

        VoucherResponse result = service.update(voucherId, lockedSameRequest(5));

        assertThat(result).isSameAs(response);
        verify(voucherRepository).save(voucherCaptor.capture());
        assertThat(voucherCaptor.getValue().getTotalQuantity()).isEqualTo(5);
    }

    // UTCID04
    @Test
    void update_voucherNotFound_shouldThrowNotFound() {
        when(voucherRepository.findById(voucherId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(voucherId, lockedSameRequest(100)))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(VoucherErrorCode.VOUCHER_NOT_FOUND);

        verify(voucherRepository, never()).save(any());
    }

    // UTCID05
    @Test
    void update_endEqualsStart_shouldThrowInvalidDateRange() {
        when(voucherRepository.findById(voucherId)).thenReturn(Optional.of(existingVoucher(0)));

        assertThatThrownBy(() -> service.update(voucherId,
                updateRequest(DiscountType.PERCENT, bd("10"), bd("50000"), bd("100000"), 100, 1, START, START)))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(VoucherErrorCode.VOUCHER_INVALID_DATE_RANGE);

        verify(voucherRepository, never()).save(any());
    }

    // UTCID06
    @Test
    void update_usageExistsInRepoAndDiscountTypeChanged_shouldThrowAlreadyUsed() {
        Voucher voucher = existingVoucher(0);
        when(voucherRepository.findById(voucherId)).thenReturn(Optional.of(voucher));
        when(usageRepository.existsByVoucherId(voucherId)).thenReturn(true);

        assertThatThrownBy(() -> service.update(voucherId,
                updateRequest(DiscountType.FIXED_AMOUNT, bd("10"), null, bd("100000"), 100, 1, START, END)))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(VoucherErrorCode.VOUCHER_ALREADY_USED_BY_USAGE);

        assertThat(voucher.getDiscountType()).isEqualTo(DiscountType.PERCENT);   // dữ liệu không bị đổi
        verify(voucherRepository, never()).save(any());
    }

    // UTCID07
    @Test
    void update_hasUsageAndDiscountValueChanged_shouldThrowAlreadyUsed() {
        Voucher voucher = existingVoucher(5);
        when(voucherRepository.findById(voucherId)).thenReturn(Optional.of(voucher));

        assertThatThrownBy(() -> service.update(voucherId,
                updateRequest(DiscountType.PERCENT, bd("15"), bd("50000"), bd("100000"), 100, 1, START, END)))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(VoucherErrorCode.VOUCHER_ALREADY_USED_BY_USAGE);

        assertThat(voucher.getDiscountValue()).isEqualByComparingTo("10");
        verify(voucherRepository, never()).save(any());
    }

    // UTCID08
    @Test
    void update_hasUsageAndMaxUsagePerCustomerChanged_shouldThrowAlreadyUsed() {
        Voucher voucher = existingVoucher(5);
        when(voucherRepository.findById(voucherId)).thenReturn(Optional.of(voucher));

        assertThatThrownBy(() -> service.update(voucherId,
                updateRequest(DiscountType.PERCENT, bd("10"), bd("50000"), bd("100000"), 100, 2, START, END)))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(VoucherErrorCode.VOUCHER_ALREADY_USED_BY_USAGE);

        assertThat(voucher.getMaxUsagePerCustomer()).isEqualTo(1);
        verify(voucherRepository, never()).save(any());
    }

    // UTCID09
    @Test
    void update_hasUsageAndSameValueDifferentScale_shouldNotTreatAsChanged() {
        Voucher voucher = existingVoucher(5);
        voucher.setDiscountValue(bd("10.00"));
        voucher.setMaxDiscountAmount(bd("50000.00"));
        voucher.setMinOrderValue(bd("100000.00"));
        VoucherResponse response = mock(VoucherResponse.class);
        when(voucherRepository.findById(voucherId)).thenReturn(Optional.of(voucher));
        stubSaveReturnsArgument();
        when(mapper.toResponse(any(Voucher.class))).thenReturn(response);

        VoucherResponse result = service.update(voucherId,
                updateRequest(DiscountType.PERCENT, bd("10.0"), bd("50000.0"), bd("100000"), 100, 1, START, END));

        assertThat(result).isSameAs(response);
        verify(voucherRepository).save(any(Voucher.class));
    }

    // UTCID10
    @Test
    void update_totalQuantityBelowUsed_shouldThrowQuantityBelowUsed() {
        Voucher voucher = existingVoucher(5);
        when(voucherRepository.findById(voucherId)).thenReturn(Optional.of(voucher));

        assertThatThrownBy(() -> service.update(voucherId, lockedSameRequest(3)))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(VoucherErrorCode.VOUCHER_QUANTITY_BELOW_USED);

        assertThat(voucher.getTotalQuantity()).isEqualTo(100);
        verify(voucherRepository, never()).save(any());
    }

    // UTCID11
    @Test
    void update_hasUsageAndMinOrderNullTreatedAsZero_shouldThrowAlreadyUsed() {
        Voucher voucher = existingVoucher(5);
        when(voucherRepository.findById(voucherId)).thenReturn(Optional.of(voucher));

        assertThatThrownBy(() -> service.update(voucherId,
                updateRequest(DiscountType.PERCENT, bd("10"), bd("50000"), null, 100, 1, START, END)))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(VoucherErrorCode.VOUCHER_ALREADY_USED_BY_USAGE);

        assertThat(voucher.getMinOrderValue()).isEqualByComparingTo("100000");
        verify(voucherRepository, never()).save(any());
    }

    // Update status
    // UTCID01
    @Test
    void updateStatus_activeToInactive_shouldDisableVoucher() {
        Voucher voucher = existingVoucher(0);
        VoucherResponse response = mock(VoucherResponse.class);
        when(voucherRepository.findById(voucherId)).thenReturn(Optional.of(voucher));
        stubSaveReturnsArgument();
        when(mapper.toResponse(any(Voucher.class))).thenReturn(response);

        VoucherResponse result = service.updateStatus(voucherId, VoucherStatus.INACTIVE);

        assertThat(result).isSameAs(response);
        verify(voucherRepository).save(voucherCaptor.capture());
        assertThat(voucherCaptor.getValue().getStatus()).isEqualTo(VoucherStatus.INACTIVE);
    }

    // UTCID02
    @Test
    void updateStatus_inactiveToActive_shouldEnableVoucher() {
        Voucher voucher = existingVoucher(0);
        voucher.setStatus(VoucherStatus.INACTIVE);
        VoucherResponse response = mock(VoucherResponse.class);
        when(voucherRepository.findById(voucherId)).thenReturn(Optional.of(voucher));
        stubSaveReturnsArgument();
        when(mapper.toResponse(any(Voucher.class))).thenReturn(response);

        VoucherResponse result = service.updateStatus(voucherId, VoucherStatus.ACTIVE);

        assertThat(result).isSameAs(response);
        verify(voucherRepository).save(voucherCaptor.capture());
        assertThat(voucherCaptor.getValue().getStatus()).isEqualTo(VoucherStatus.ACTIVE);
    }

    // UTCID03
    @Test
    void updateStatus_voucherNotFound_shouldThrowNotFound() {
        when(voucherRepository.findById(voucherId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateStatus(voucherId, VoucherStatus.INACTIVE))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(VoucherErrorCode.VOUCHER_NOT_FOUND);

        verify(voucherRepository, never()).save(any());
        verifyNoInteractions(mapper);
    }

    // Get all
    // UTCID01
    @Test
    void getAll_withStatusAndKeyword_shouldReturnMappedPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Voucher voucher = existingVoucher(0);
        VoucherResponse response = mock(VoucherResponse.class);
        when(voucherRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(voucher), pageable, 1));
        when(mapper.toResponse(voucher)).thenReturn(response);

        Page<VoucherResponse> result = service.getAll(VoucherDisplayStatus.RUNNING, "sale", pageable);

        assertThat(result.getContent()).containsExactly(response);
        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(voucherRepository).findAll(any(Specification.class), eq(pageable));
    }

    // UTCID02
    @Test
    void getAll_noFilter_shouldReturnMappedPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Voucher voucher = existingVoucher(0);
        VoucherResponse response = mock(VoucherResponse.class);
        when(voucherRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(voucher), pageable, 1));
        when(mapper.toResponse(voucher)).thenReturn(response);

        Page<VoucherResponse> result = service.getAll(null, null, pageable);

        assertThat(result.getContent()).containsExactly(response);
        verify(voucherRepository).findAll(any(Specification.class), eq(pageable));
    }

    // UTCID03
    @Test
    void getAll_emptyPage_shouldReturnEmptyPage() {
        Pageable pageable = PageRequest.of(0, 10);
        when(voucherRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(Page.empty(pageable));

        Page<VoucherResponse> result = service.getAll(null, null, pageable);

        assertThat(result.getContent()).isEmpty();
        verifyNoInteractions(mapper);
    }

    // Get by id
    // UTCID01
    @Test
    void getById_voucherExists_shouldReturnResponse() {
        Voucher voucher = existingVoucher(0);
        VoucherResponse response = mock(VoucherResponse.class);
        when(voucherRepository.findById(voucherId)).thenReturn(Optional.of(voucher));
        when(mapper.toResponse(voucher)).thenReturn(response);

        VoucherResponse result = service.getById(voucherId);

        assertThat(result).isSameAs(response);
    }

    // UTCID02
    @Test
    void getById_voucherNotFound_shouldThrowNotFound() {
        when(voucherRepository.findById(voucherId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(voucherId))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(VoucherErrorCode.VOUCHER_NOT_FOUND);

        verifyNoInteractions(mapper);
    }

    // Get usage
    // UTCID01
    @Test
    void getUsages_voucherExists_shouldReturnMappedPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Voucher voucher = existingVoucher(1);
        VoucherUsage usage = usageOf(voucher);
        VoucherUsageResponse response = mock(VoucherUsageResponse.class);
        when(voucherRepository.findById(voucherId)).thenReturn(Optional.of(voucher));
        when(usageRepository.findByVoucherId(voucherId, pageable))
                .thenReturn(new PageImpl<>(List.of(usage), pageable, 1));
        when(mapper.toUsageResponse(usage)).thenReturn(response);

        Page<VoucherUsageResponse> result = service.getUsages(voucherId, pageable);

        assertThat(result.getContent()).containsExactly(response);
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    // UTCID02
    @Test
    void getUsages_voucherNotFound_shouldThrowNotFound() {
        Pageable pageable = PageRequest.of(0, 10);
        when(voucherRepository.findById(voucherId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getUsages(voucherId, pageable))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(VoucherErrorCode.VOUCHER_NOT_FOUND);

        verifyNoInteractions(usageRepository);
        verifyNoInteractions(mapper);
    }

    // UTCID03
    @Test
    void getUsages_noUsage_shouldReturnEmptyPage() {
        Pageable pageable = PageRequest.of(0, 10);
        when(voucherRepository.findById(voucherId)).thenReturn(Optional.of(existingVoucher(0)));
        when(usageRepository.findByVoucherId(voucherId, pageable)).thenReturn(Page.empty(pageable));

        Page<VoucherUsageResponse> result = service.getUsages(voucherId, pageable);

        assertThat(result.getContent()).isEmpty();
        verifyNoInteractions(mapper);
    }

    // Get available
    // UTCID01
    @Test
    void getAvailable_vouchersAvailable_shouldReturnMappedPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Voucher voucher = existingVoucher(0);
        VoucherSummaryResponse summary = mock(VoucherSummaryResponse.class);
        when(voucherRepository.findAvailable(any(LocalDateTime.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(voucher), pageable, 1));
        when(mapper.toSummary(voucher)).thenReturn(summary);

        Page<VoucherSummaryResponse> result = service.getAvailable(pageable);

        assertThat(result.getContent()).containsExactly(summary);
        verify(voucherRepository).findAvailable(any(LocalDateTime.class), eq(pageable));
    }

    // UTCID02
    @Test
    void getAvailable_noneAvailable_shouldReturnEmptyPage() {
        Pageable pageable = PageRequest.of(0, 10);
        when(voucherRepository.findAvailable(any(LocalDateTime.class), eq(pageable)))
                .thenReturn(Page.empty(pageable));

        Page<VoucherSummaryResponse> result = service.getAvailable(pageable);

        assertThat(result.getContent()).isEmpty();
        verifyNoInteractions(mapper);
    }

    // Calculate
    // UTCID01
    @Test
    void calculate_validVoucher_shouldNormalizeCodeAndReturnPreview() {
        Voucher voucher = runningVoucher(0);
        when(voucherRepository.findByCode("SALE10")).thenReturn(Optional.of(voucher));
        stubUsedByCustomer(0);

        VoucherPreviewResponse result = service.calculate("  sale10 ", customerId, bd("200000"));

        assertThat(result.voucherId()).isEqualTo(voucherId);
        assertThat(result.code()).isEqualTo("SALE10");
        assertThat(result.name()).isEqualTo("Old Name");
        assertThat(result.discountType()).isEqualTo(DiscountType.PERCENT);
        assertThat(result.discountValue()).isEqualByComparingTo("10");
        assertThat(result.orderValue()).isEqualByComparingTo("200000");
        assertThat(result.discountAmount()).isEqualByComparingTo("20000");
        assertThat(result.amountAfterVoucher()).isEqualByComparingTo("180000");
    }

    // UTCID02
    @Test
    void calculate_voucherNotFound_shouldThrowNotFound() {
        when(voucherRepository.findByCode("SALE10")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.calculate("sale10", customerId, bd("200000")))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(VoucherErrorCode.VOUCHER_NOT_FOUND);

        verifyNoInteractions(usageRepository);
    }

    // UTCID03
    @Test
    void calculate_inactiveVoucher_shouldThrowNotActive() {
        Voucher voucher = runningVoucher(0);
        voucher.setStatus(VoucherStatus.INACTIVE);
        when(voucherRepository.findByCode("SALE10")).thenReturn(Optional.of(voucher));

        assertThatThrownBy(() -> service.calculate("SALE10", customerId, bd("200000")))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(VoucherErrorCode.VOUCHER_NOT_ACTIVE);

        verifyNoInteractions(usageRepository);
    }

    // UTCID04
    @Test
    void calculate_notStartedVoucher_shouldThrowNotStarted() {
        Voucher voucher = runningVoucher(0);
        voucher.setStartDate(LocalDateTime.of(2098, 1, 1, 0, 0));
        when(voucherRepository.findByCode("SALE10")).thenReturn(Optional.of(voucher));

        assertThatThrownBy(() -> service.calculate("SALE10", customerId, bd("200000")))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(VoucherErrorCode.VOUCHER_NOT_STARTED);

        verifyNoInteractions(usageRepository);
    }

    // UTCID05
    @Test
    void calculate_expiredVoucher_shouldThrowExpired() {
        Voucher voucher = runningVoucher(0);
        voucher.setEndDate(LocalDateTime.of(2021, 1, 1, 0, 0));
        when(voucherRepository.findByCode("SALE10")).thenReturn(Optional.of(voucher));

        assertThatThrownBy(() -> service.calculate("SALE10", customerId, bd("200000")))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(VoucherErrorCode.VOUCHER_EXPIRED);

        verifyNoInteractions(usageRepository);
    }

    // UTCID06
    @Test
    void calculate_outOfStock_shouldThrowOutOfStock() {
        Voucher voucher = runningVoucher(100);   // used 100 / total 100
        when(voucherRepository.findByCode("SALE10")).thenReturn(Optional.of(voucher));

        assertThatThrownBy(() -> service.calculate("SALE10", customerId, bd("200000")))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(VoucherErrorCode.VOUCHER_OUT_OF_STOCK);

        verifyNoInteractions(usageRepository);
    }

    // UTCID07
    @Test
    void calculate_belowMinOrder_shouldThrowMinOrderNotMet() {
        Voucher voucher = runningVoucher(0);
        when(voucherRepository.findByCode("SALE10")).thenReturn(Optional.of(voucher));

        assertThatThrownBy(() -> service.calculate("SALE10", customerId, bd("99999")))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(VoucherErrorCode.VOUCHER_MIN_ORDER_NOT_MET);

        verifyNoInteractions(usageRepository);
    }

    // UTCID08
    @Test
    void calculate_baseEqualsMinOrder_shouldBeAccepted() {
        Voucher voucher = runningVoucher(0);
        when(voucherRepository.findByCode("SALE10")).thenReturn(Optional.of(voucher));
        stubUsedByCustomer(0);

        VoucherPreviewResponse result = service.calculate("SALE10", customerId, bd("100000"));

        assertThat(result.discountAmount()).isEqualByComparingTo("10000");
        assertThat(result.amountAfterVoucher()).isEqualByComparingTo("90000");
    }

    // UTCID09
    @Test
    void calculate_customerReachedLimit_shouldThrowUsageLimitReached() {
        Voucher voucher = runningVoucher(0);   // maxUsagePerCustomer = 1
        when(voucherRepository.findByCode("SALE10")).thenReturn(Optional.of(voucher));
        stubUsedByCustomer(1);

        assertThatThrownBy(() -> service.calculate("SALE10", customerId, bd("200000")))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(VoucherErrorCode.VOUCHER_USAGE_LIMIT_REACHED);
    }

    // UTCID10
    @Test
    void calculate_customerBelowLimit_shouldReturnPreview() {
        Voucher voucher = runningVoucher(0);
        voucher.setMaxUsagePerCustomer(3);
        when(voucherRepository.findByCode("SALE10")).thenReturn(Optional.of(voucher));
        stubUsedByCustomer(1);

        VoucherPreviewResponse result = service.calculate("SALE10", customerId, bd("200000"));

        assertThat(result.discountAmount()).isEqualByComparingTo("20000");
        assertThat(result.amountAfterVoucher()).isEqualByComparingTo("180000");
    }

    // UTCID11
    @Test
    void calculate_percentExceedsCap_shouldCapDiscount() {
        Voucher voucher = runningVoucher(0);   // 10%, trần 50000
        when(voucherRepository.findByCode("SALE10")).thenReturn(Optional.of(voucher));
        stubUsedByCustomer(0);

        VoucherPreviewResponse result = service.calculate("SALE10", customerId, bd("1000000"));

        assertThat(result.discountAmount()).isEqualByComparingTo("50000");
        assertThat(result.amountAfterVoucher()).isEqualByComparingTo("950000");
    }

    // Consume
    // UTCID01
    @Test
    void consume_stockAvailable_shouldSaveUsage() {
        Voucher voucher = existingVoucher(1);
        when(voucherRepository.increaseUsed(voucherId)).thenReturn(1);
        when(voucherRepository.getReferenceById(voucherId)).thenReturn(voucher);
        stubUsedByCustomer(0);

        service.consume(voucherId, customerId, orderId, bd("20000"));

        verify(usageRepository).save(usageCaptor.capture());
        VoucherUsage saved = usageCaptor.getValue();
        assertThat(saved.getVoucher()).isSameAs(voucher);
        assertThat(saved.getCustomerId()).isEqualTo(customerId);
        assertThat(saved.getOrderId()).isEqualTo(orderId);
        assertThat(saved.getDiscountAmount()).isEqualByComparingTo("20000");
        assertThat(saved.getStatus()).isEqualTo(VoucherUsageStatus.USED);
        assertThat(saved.getUsedAt()).isNotNull();
    }

    // UTCID02
    @Test
    void consume_outOfStock_shouldThrowOutOfStock() {
        when(voucherRepository.increaseUsed(voucherId)).thenReturn(0);

        assertThatThrownBy(() -> service.consume(voucherId, customerId, orderId, bd("20000")))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(VoucherErrorCode.VOUCHER_OUT_OF_STOCK);

        verify(voucherRepository, never()).getReferenceById(any());
        verifyNoInteractions(usageRepository);
    }

    // UTCID03
    @Test
    void consume_customerReachedLimit_shouldThrowUsageLimitReached() {
        Voucher voucher = existingVoucher(1);   // maxUsagePerCustomer = 1
        when(voucherRepository.increaseUsed(voucherId)).thenReturn(1);
        when(voucherRepository.getReferenceById(voucherId)).thenReturn(voucher);
        stubUsedByCustomer(1);

        assertThatThrownBy(() -> service.consume(voucherId, customerId, orderId, bd("20000")))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(VoucherErrorCode.VOUCHER_USAGE_LIMIT_REACHED);

        verify(usageRepository, never()).save(any());
    }

    // UTCID04
    @Test
    void consume_oneUsageLeft_shouldSaveUsage() {
        Voucher voucher = existingVoucher(1);
        voucher.setMaxUsagePerCustomer(3);
        when(voucherRepository.increaseUsed(voucherId)).thenReturn(1);
        when(voucherRepository.getReferenceById(voucherId)).thenReturn(voucher);
        stubUsedByCustomer(2);

        service.consume(voucherId, customerId, orderId, bd("20000"));

        verify(usageRepository).save(any(VoucherUsage.class));
    }

    // Refund
    // UTCID01
    @Test
    void refund_usedUsage_shouldMarkRefundedAndDecreaseUsed() {
        Voucher voucher = existingVoucher(1);
        VoucherUsage usage = usageOf(voucher);   // status mặc định USED
        when(usageRepository.findByOrderIdAndStatus(orderId, VoucherUsageStatus.USED))
                .thenReturn(Optional.of(usage));

        service.refund(orderId);

        verify(usageRepository).save(usageCaptor.capture());
        VoucherUsage saved = usageCaptor.getValue();
        assertThat(saved.getStatus()).isEqualTo(VoucherUsageStatus.REFUNDED);
        assertThat(saved.getRefundedAt()).isNotNull();
        verify(voucherRepository).decreaseUsed(voucherId);
    }

    // UTCID02
    @Test
    void refund_noUsedUsage_shouldDoNothing() {
        when(usageRepository.findByOrderIdAndStatus(orderId, VoucherUsageStatus.USED))
                .thenReturn(Optional.empty());

        service.refund(orderId);

        verify(usageRepository, never()).save(any());
        verifyNoInteractions(voucherRepository);
    }
}
