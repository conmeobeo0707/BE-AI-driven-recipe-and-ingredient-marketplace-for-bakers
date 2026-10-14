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
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ReceiptVoucherServiceImpl implements ReceiptVoucherService{

    ReceiptVoucherRepository voucherRepository;
    ReceiptCategoryRepository categoryRepository;
    ReceiptVoucherMapper mapper;

    @Override
    @Transactional
    public ReceiptVoucherResponse create(UUID userId, CreateReceiptVoucherRequest request) {
        ReceiptCategory category = categoryRepository.findById(request.receiptCategoryId())
                .orElseThrow(() -> new AppException(ReceiptCategoryErrorCode.RECEIPT_CATEGORY_NOT_FOUND));

        if (category.getType() != ReceiptCategoryType.OTHER) {
            throw new AppException(ReceiptVoucherErrorCode.RECEIPT_CATEGORY_NOT_MANUAL);
        }

        ReceiptVoucher voucher = ReceiptVoucher.builder()
                .code(generateCode())
                .receiptCategory(category)
                .amount(request.amount())
                .createdBy(userId)
                .note(request.note())
                .build();

        return mapper.toResponse(voucherRepository.save(voucher));
    }

    @Override
    @Transactional
    public ReceiptVoucherResponse createFromPayment(UUID paymentId, BigDecimal amount, ReceiptCategoryType type, String note) {
        return voucherRepository.findByPaymentId(paymentId)
                .map(mapper::toResponse)
                .orElseGet(() -> {
                    ReceiptCategory category = categoryRepository.findFirstByTypeOrderByCreatedAtAsc(type)
                            .orElseThrow(() -> new AppException(ReceiptVoucherErrorCode.SYSTEM_RECEIPT_CATEGORY_MISSING));

                    ReceiptVoucher voucher = ReceiptVoucher.builder()
                            .code(generateCode())
                            .receiptCategory(category)
                            .paymentId(paymentId)
                            .amount(amount)
                            .note(note)
                            .build();
                    return mapper.toResponse(voucherRepository.save(voucher));
                });
    }

    @Override
    @Transactional(readOnly = true)
    public ReceiptVoucherResponse getById(UUID id) {
        return voucherRepository.findWithCategoryById(id).map(mapper::toResponse)
                .orElseThrow(() -> new AppException(ReceiptVoucherErrorCode.RECEIPT_VOUCHER_NOT_FOUND));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReceiptVoucherResponse> getAll(UUID categoryId, Pageable pageable) {
        Page<ReceiptVoucher> page = categoryId == null
                ? voucherRepository.findAll(pageable)
                : voucherRepository.findByReceiptCategoryId(categoryId, pageable);
        return page.map(mapper::toResponse);
    }

    private String generateCode() {
        String code;
        do {
            code = "PT-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        } while (voucherRepository.existsByCode(code));
        return code;
    }
}
