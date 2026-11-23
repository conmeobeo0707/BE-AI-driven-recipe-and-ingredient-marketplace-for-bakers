package com.manhnv.capstonebakerai.receipt.service;

import com.manhnv.capstonebakerai.common.exception.AppException;
import com.manhnv.capstonebakerai.receipt.dto.request.CreateReceiptVoucherRequest;
import com.manhnv.capstonebakerai.receipt.dto.response.ReceiptVoucherResponse;
import com.manhnv.capstonebakerai.receipt.entity.ReceiptCategory;
import com.manhnv.capstonebakerai.receipt.entity.ReceiptVoucher;
import com.manhnv.capstonebakerai.receipt.enums.ReceiptCategoryType;
import com.manhnv.capstonebakerai.receipt.exception.ReceiptCategoryErrorCode;
import com.manhnv.capstonebakerai.receipt.exception.ReceiptVoucherErrorCode;
import com.manhnv.capstonebakerai.receipt.mapper.ReceiptVoucherMapper;
import com.manhnv.capstonebakerai.receipt.repository.ReceiptCategoryRepository;
import com.manhnv.capstonebakerai.receipt.repository.ReceiptVoucherRepository;
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

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReceiptVoucherServiceImplTest {

    @Mock
    ReceiptVoucherRepository voucherRepository;
    @Mock
    ReceiptCategoryRepository categoryRepository;
    @Mock
    ReceiptVoucherMapper mapper;
    @InjectMocks
    ReceiptVoucherServiceImpl service;

    @Captor
    ArgumentCaptor<ReceiptVoucher> voucherCaptor;

    private final UUID userId = UUID.randomUUID();
    private final UUID categoryId = UUID.randomUUID();
    private final UUID paymentId = UUID.randomUUID();
    private final UUID voucherId = UUID.randomUUID();

    // Loại không phải OTHER (loại hệ thống), lấy từ values() để không phụ thuộc tên hằng
    private static final ReceiptCategoryType SYSTEM_TYPE = Arrays.stream(ReceiptCategoryType.values())
            .filter(t -> t != ReceiptCategoryType.OTHER)
            .findFirst()
            .orElseThrow();

    private ReceiptCategory categoryOf(ReceiptCategoryType type) {
        return ReceiptCategory.builder().id(categoryId).name("Category").type(type).build();
    }

    private ReceiptVoucher voucherOf(ReceiptCategory category) {
        return ReceiptVoucher.builder()
                .id(voucherId)
                .code("PT-ABC123")
                .receiptCategory(category)
                .amount(new BigDecimal("100000"))
                .build();
    }

    private void stubSaveReturnsArgument() {
        when(voucherRepository.save(any(ReceiptVoucher.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    // Create
    // UTCID01
    @Test
    void create_manualCategory_shouldSaveVoucher() {
        ReceiptCategory category = categoryOf(ReceiptCategoryType.OTHER);
        ReceiptVoucherResponse response = mock(ReceiptVoucherResponse.class);
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(voucherRepository.existsByCode(anyString())).thenReturn(false);
        stubSaveReturnsArgument();
        when(mapper.toResponse(any(ReceiptVoucher.class))).thenReturn(response);

        ReceiptVoucherResponse result = service.create(userId,
                new CreateReceiptVoucherRequest(categoryId, new BigDecimal("150000"), "Thu khac"));

        assertThat(result).isSameAs(response);
        verify(voucherRepository).save(voucherCaptor.capture());
        ReceiptVoucher saved = voucherCaptor.getValue();
        assertThat(saved.getCode()).matches("PT-[0-9A-F]{6}");
        assertThat(saved.getReceiptCategory()).isSameAs(category);
        assertThat(saved.getAmount()).isEqualByComparingTo("150000");
        assertThat(saved.getCreatedBy()).isEqualTo(userId);
        assertThat(saved.getNote()).isEqualTo("Thu khac");
        assertThat(saved.getPaymentId()).isNull();
    }

    // UTCID02
    @Test
    void create_generatedCodeCollides_shouldGenerateAgain() {
        ReceiptCategory category = categoryOf(ReceiptCategoryType.OTHER);
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(voucherRepository.existsByCode(anyString())).thenReturn(true, false);
        stubSaveReturnsArgument();
        when(mapper.toResponse(any(ReceiptVoucher.class))).thenReturn(mock(ReceiptVoucherResponse.class));

        service.create(userId, new CreateReceiptVoucherRequest(categoryId, new BigDecimal("150000"), "note"));

        verify(voucherRepository, times(2)).existsByCode(anyString());
        verify(voucherRepository).save(any(ReceiptVoucher.class));
    }

    // UTCID03
    @Test
    void create_categoryNotFound_shouldThrowCategoryNotFound() {
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(userId,
                new CreateReceiptVoucherRequest(categoryId, new BigDecimal("150000"), "note")))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(ReceiptCategoryErrorCode.RECEIPT_CATEGORY_NOT_FOUND);

        verify(voucherRepository, never()).save(any());
    }

    // UTCID04
    @Test
    void create_systemCategory_shouldThrowNotManual() {
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(categoryOf(SYSTEM_TYPE)));

        assertThatThrownBy(() -> service.create(userId,
                new CreateReceiptVoucherRequest(categoryId, new BigDecimal("150000"), "note")))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(ReceiptVoucherErrorCode.RECEIPT_CATEGORY_NOT_MANUAL);

        verify(voucherRepository, never()).save(any());
        verify(voucherRepository, never()).existsByCode(any());
    }

    // Create from payment
    // UTCID01
    @Test
    void createFromPayment_noExistingVoucher_shouldCreateVoucher() {
        ReceiptCategory category = categoryOf(SYSTEM_TYPE);
        ReceiptVoucherResponse response = mock(ReceiptVoucherResponse.class);
        when(voucherRepository.findByPaymentId(paymentId)).thenReturn(Optional.empty());
        when(categoryRepository.findFirstByTypeOrderByCreatedAtAsc(SYSTEM_TYPE)).thenReturn(Optional.of(category));
        when(voucherRepository.existsByCode(anyString())).thenReturn(false);
        stubSaveReturnsArgument();
        when(mapper.toResponse(any(ReceiptVoucher.class))).thenReturn(response);

        ReceiptVoucherResponse result = service.createFromPayment(paymentId, new BigDecimal("200000"), SYSTEM_TYPE, "Thanh toan don");

        assertThat(result).isSameAs(response);
        verify(voucherRepository).save(voucherCaptor.capture());
        ReceiptVoucher saved = voucherCaptor.getValue();
        assertThat(saved.getCode()).matches("PT-[0-9A-F]{6}");
        assertThat(saved.getReceiptCategory()).isSameAs(category);
        assertThat(saved.getPaymentId()).isEqualTo(paymentId);
        assertThat(saved.getAmount()).isEqualByComparingTo("200000");
        assertThat(saved.getNote()).isEqualTo("Thanh toan don");
        assertThat(saved.getCreatedBy()).isNull();
    }

    // UTCID02
    @Test
    void createFromPayment_voucherAlreadyExists_shouldReturnExistingWithoutCreating() {
        ReceiptVoucher existing = voucherOf(categoryOf(SYSTEM_TYPE));
        ReceiptVoucherResponse response = mock(ReceiptVoucherResponse.class);
        when(voucherRepository.findByPaymentId(paymentId)).thenReturn(Optional.of(existing));
        when(mapper.toResponse(existing)).thenReturn(response);

        ReceiptVoucherResponse result = service.createFromPayment(paymentId, new BigDecimal("200000"), SYSTEM_TYPE, "note");

        assertThat(result).isSameAs(response);
        verifyNoInteractions(categoryRepository);
        verify(voucherRepository, never()).save(any());
    }

    // UTCID03
    @Test
    void createFromPayment_systemCategoryMissing_shouldThrowMissing() {
        when(voucherRepository.findByPaymentId(paymentId)).thenReturn(Optional.empty());
        when(categoryRepository.findFirstByTypeOrderByCreatedAtAsc(SYSTEM_TYPE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createFromPayment(paymentId, new BigDecimal("200000"), SYSTEM_TYPE, "note"))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(ReceiptVoucherErrorCode.SYSTEM_RECEIPT_CATEGORY_MISSING);

        verify(voucherRepository, never()).save(any());
    }

    // Get by id
    // UTCID01
    @Test
    void getById_voucherExists_shouldReturnResponse() {
        ReceiptVoucher voucher = voucherOf(categoryOf(ReceiptCategoryType.OTHER));
        ReceiptVoucherResponse response = mock(ReceiptVoucherResponse.class);
        when(voucherRepository.findWithCategoryById(voucherId)).thenReturn(Optional.of(voucher));
        when(mapper.toResponse(voucher)).thenReturn(response);

        ReceiptVoucherResponse result = service.getById(voucherId);

        assertThat(result).isSameAs(response);
    }

    // UTCID02
    @Test
    void getById_voucherNotFound_shouldThrowNotFound() {
        when(voucherRepository.findWithCategoryById(voucherId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(voucherId))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(ReceiptVoucherErrorCode.RECEIPT_VOUCHER_NOT_FOUND);

        verifyNoInteractions(mapper);
    }

    // Get all
    // UTCID01
    @Test
    void getAll_categoryIdNull_shouldReturnAllVouchers() {
        Pageable pageable = PageRequest.of(0, 10);
        ReceiptVoucher voucher = voucherOf(categoryOf(ReceiptCategoryType.OTHER));
        ReceiptVoucherResponse response = mock(ReceiptVoucherResponse.class);
        when(voucherRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(voucher), pageable, 1));
        when(mapper.toResponse(voucher)).thenReturn(response);

        Page<ReceiptVoucherResponse> result = service.getAll(null, pageable);

        assertThat(result.getContent()).containsExactly(response);
        verify(voucherRepository, never()).findByReceiptCategoryId(any(), any());
    }

    // UTCID02
    @Test
    void getAll_withCategoryId_shouldReturnVouchersOfCategory() {
        Pageable pageable = PageRequest.of(0, 10);
        ReceiptVoucher voucher = voucherOf(categoryOf(ReceiptCategoryType.OTHER));
        ReceiptVoucherResponse response = mock(ReceiptVoucherResponse.class);
        when(voucherRepository.findByReceiptCategoryId(categoryId, pageable))
                .thenReturn(new PageImpl<>(List.of(voucher), pageable, 1));
        when(mapper.toResponse(voucher)).thenReturn(response);

        Page<ReceiptVoucherResponse> result = service.getAll(categoryId, pageable);

        assertThat(result.getContent()).containsExactly(response);
        verify(voucherRepository, never()).findAll(any(Pageable.class));
    }

    // UTCID03
    @Test
    void getAll_categoryWithoutVoucher_shouldReturnEmptyPage() {
        Pageable pageable = PageRequest.of(0, 10);
        when(voucherRepository.findByReceiptCategoryId(categoryId, pageable)).thenReturn(Page.empty(pageable));

        Page<ReceiptVoucherResponse> result = service.getAll(categoryId, pageable);

        assertThat(result.getContent()).isEmpty();
        verifyNoInteractions(mapper);
    }
}
