package com.manhnv.capstonebakerai.receipt.service;

import com.manhnv.capstonebakerai.receipt.dto.request.CreateReceiptVoucherRequest;
import com.manhnv.capstonebakerai.receipt.dto.response.ReceiptVoucherResponse;
import com.manhnv.capstonebakerai.receipt.enums.ReceiptCategoryType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.UUID;

public interface ReceiptVoucherService {

    ReceiptVoucherResponse create(UUID userId, CreateReceiptVoucherRequest request);
    ReceiptVoucherResponse createFromPayment(UUID paymentId, BigDecimal amount, ReceiptCategoryType type, String note);
    ReceiptVoucherResponse getById(UUID id);
    Page<ReceiptVoucherResponse> getAll(UUID categoryId, Pageable pageable);
}
