package com.manhnv.capstonebakerai.voucher.service;

import com.manhnv.capstonebakerai.voucher.dto.request.CreateVoucherRequest;
import com.manhnv.capstonebakerai.voucher.dto.request.UpdateVoucherRequest;
import com.manhnv.capstonebakerai.voucher.dto.response.VoucherPreviewResponse;
import com.manhnv.capstonebakerai.voucher.dto.response.VoucherResponse;
import com.manhnv.capstonebakerai.voucher.dto.response.VoucherSummaryResponse;
import com.manhnv.capstonebakerai.voucher.dto.response.VoucherUsageResponse;
import com.manhnv.capstonebakerai.voucher.enums.VoucherDisplayStatus;
import com.manhnv.capstonebakerai.voucher.enums.VoucherStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.UUID;

public interface VoucherService {
    VoucherResponse create(UUID managerUserId, CreateVoucherRequest request);
    VoucherResponse update(UUID id, UpdateVoucherRequest request);
    VoucherResponse updateStatus(UUID id, VoucherStatus status);
    Page<VoucherResponse> getAll(VoucherDisplayStatus status, String keyword, Pageable pageable);
    VoucherResponse getById(UUID id);
    Page<VoucherUsageResponse> getUsages(UUID id, Pageable pageable);

    // === Public / Customer ===
    Page<VoucherSummaryResponse> getAvailable(Pageable pageable);

    // === Dùng nội bộ cho module Order ===
    /** Kiểm tra voucher hợp lệ và tính mức giảm trên `base`. Không ghi DB. */
    VoucherPreviewResponse calculate(String code, UUID customerId, BigDecimal base);

    /** Ghi nhận dùng voucher cho đơn. Phải chạy cùng transaction với việc tạo đơn. */
    void consume(UUID voucherId, UUID customerId, UUID orderId, BigDecimal discountAmount);

    /** Hoàn voucher khi đơn bị hủy. Idempotent: gọi nhiều lần vẫn an toàn. */
    void refund(UUID orderId);
}
