package com.manhnv.capstonebakerai.voucher.service;

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
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import jakarta.persistence.criteria.Predicate;


@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class VoucherServiceImpl implements VoucherService {

    VoucherRepository voucherRepository;
    VoucherUsageRepository usageRepository;
    VoucherMapper mapper;

    private static final String CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // bỏ O,0,I,1 cho dễ đọc
    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    @Transactional
    public VoucherResponse create(UUID managerUserId, CreateVoucherRequest request) {
        String code;
        if (request.code() == null || request.code().isBlank()) {
            code = generateCode();
        } else {
            code = normalize(request.code());
            if (voucherRepository.existsByCode(code)) {
                throw new AppException(VoucherErrorCode.VOUCHER_CODE_EXISTED);
            }
        }
        validateRules(request.discountType(), request.discountValue(), request.startDate(), request.endDate());

        Voucher voucher = Voucher.builder()
                .code(code)
                .name(request.name().trim())
                .description(request.description())
                .discountType(request.discountType())
                .discountValue(request.discountValue())
                .maxDiscountAmount(resolveMaxDiscount(request.discountType(), request.maxDiscountAmount()))
                .minOrderValue(orZero(request.minOrderValue()))
                .totalQuantity(request.totalQuantity())
                .maxUsagePerCustomer(request.maxUsagePerCustomer())
                .startDate(request.startDate())
                .endDate(request.endDate())
                .createdBy(managerUserId)
                .build();
        return mapper.toResponse(voucherRepository.save(voucher));
    }

    @Override
    @Transactional
    public VoucherResponse update(UUID id, UpdateVoucherRequest request) {
        Voucher voucher = findOrThrow(id);
        validateRules(request.discountType(), request.discountValue(), request.startDate(), request.endDate());

        BigDecimal maxDiscount = resolveMaxDiscount(request.discountType(), request.maxDiscountAmount());
        BigDecimal minOrder = orZero(request.minOrderValue());

        boolean hasUsage = voucher.getUsedQuantity() > 0 || usageRepository.existsByVoucherId(id);
        if (hasUsage) {
            boolean lockedChanged = voucher.getDiscountType() != request.discountType()
                    || differs(voucher.getDiscountValue(), request.discountValue())
                    || differs(voucher.getMinOrderValue(), minOrder)
                    || differs(voucher.getMaxDiscountAmount(), maxDiscount)
                    || !voucher.getMaxUsagePerCustomer().equals(request.maxUsagePerCustomer());
            if (lockedChanged) {
                throw new AppException(VoucherErrorCode.VOUCHER_ALREADY_USED_BY_USAGE);
            }
        }
        if (request.totalQuantity() < voucher.getUsedQuantity()) {
            throw new AppException(VoucherErrorCode.VOUCHER_QUANTITY_BELOW_USED);
        }

        voucher.setName(request.name().trim());
        voucher.setDescription(request.description());
        voucher.setDiscountType(request.discountType());
        voucher.setDiscountValue(request.discountValue());
        voucher.setMaxDiscountAmount(maxDiscount);
        voucher.setMinOrderValue(minOrder);
        voucher.setTotalQuantity(request.totalQuantity());
        voucher.setMaxUsagePerCustomer(request.maxUsagePerCustomer());
        voucher.setStartDate(request.startDate());
        voucher.setEndDate(request.endDate());
        return mapper.toResponse(voucherRepository.save(voucher));
    }

    @Override
    @Transactional
    public VoucherResponse updateStatus(UUID id, VoucherStatus status) {
        Voucher voucher = findOrThrow(id);
        voucher.setStatus(status);
        return mapper.toResponse(voucherRepository.save(voucher));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<VoucherResponse> getAll(VoucherDisplayStatus status, String keyword, Pageable pageable) {
        return voucherRepository.findAll(spec(status, keyword, LocalDateTime.now()), pageable)
                .map(mapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public VoucherResponse getById(UUID id) {
        return mapper.toResponse(findOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<VoucherUsageResponse> getUsages(UUID id, Pageable pageable) {
        findOrThrow(id);
        return usageRepository.findByVoucherId(id, pageable).map(mapper::toUsageResponse);
    }

    // ======================= PUBLIC =======================

    @Override
    @Transactional(readOnly = true)
    public Page<VoucherSummaryResponse> getAvailable(Pageable pageable) {
        return voucherRepository.findAvailable(LocalDateTime.now(), pageable).map(mapper::toSummary);
    }

    // ======================= DÙNG CHO ORDER =======================

    @Override
    @Transactional(readOnly = true)
    public VoucherPreviewResponse calculate(String code, UUID customerId, BigDecimal base) {
        Voucher voucher = voucherRepository.findByCode(normalize(code))
                .orElseThrow(() -> new AppException(VoucherErrorCode.VOUCHER_NOT_FOUND));

        validateUsable(voucher, base, LocalDateTime.now());

        long usedByCustomer = usageRepository.countByVoucherIdAndCustomerIdAndStatus(voucher.getId(), customerId, VoucherUsageStatus.USED);
        if (usedByCustomer >= voucher.getMaxUsagePerCustomer()) {
            throw new AppException(VoucherErrorCode.VOUCHER_USAGE_LIMIT_REACHED);
        }

        BigDecimal discount = voucher.calculateDiscount(base);
        return new VoucherPreviewResponse(
                voucher.getId(), voucher.getCode(), voucher.getName(),
                voucher.getDiscountType(), voucher.getDiscountValue(),
                base, discount, base.subtract(discount)
        );
    }

    @Override
    @Transactional
    public void consume(UUID voucherId, UUID customerId, UUID orderId, BigDecimal discountAmount) {
        if (voucherRepository.increaseUsed(voucherId) == 0) {
            throw new AppException(VoucherErrorCode.VOUCHER_OUT_OF_STOCK);
        }

        Voucher voucher = voucherRepository.getReferenceById(voucherId);
        long usedByCustomer = usageRepository.countByVoucherIdAndCustomerIdAndStatus(voucherId, customerId, VoucherUsageStatus.USED);
        if (usedByCustomer >= voucher.getMaxUsagePerCustomer()) {
            throw new AppException(VoucherErrorCode.VOUCHER_USAGE_LIMIT_REACHED);
        }

        usageRepository.save(VoucherUsage.builder()
                        .voucher(voucher)
                        .customerId(customerId)
                        .orderId(orderId)
                        .discountAmount(discountAmount)
                        .usedAt(LocalDateTime.now())
                .build());
    }

    @Override
    @Transactional
    public void refund(UUID orderId) {
        usageRepository.findByOrderIdAndStatus(orderId, VoucherUsageStatus.USED).ifPresent(usage -> {
            usage.setStatus(VoucherUsageStatus.REFUNDED);
            usage.setRefundedAt(LocalDateTime.now());
            usageRepository.save(usage);
            voucherRepository.decreaseUsed(usage.getVoucher().getId());
            log.info("Refunded voucher usage for order {}", orderId);
        });
    }

    private String generateCode() {
        String code;
        do {
            StringBuilder sb = new StringBuilder("VC");
            for (int i = 0; i < 8; i++) {
                sb.append(CODE_CHARS.charAt(RANDOM.nextInt(CODE_CHARS.length())));
            }
            code = sb.toString();
        } while (voucherRepository.existsByCode(code));
        return code;
    }

    /** maxDiscountAmount chỉ có ý nghĩa với PERCENT. */
    private BigDecimal resolveMaxDiscount(DiscountType type, BigDecimal max) {
        return type == DiscountType.PERCENT ? max : null;
    }

    private BigDecimal orZero(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    private boolean differs(BigDecimal a, BigDecimal b) {
        if (a == null && b == null) return false;
        if (a == null || b == null) return true;
        return a.compareTo(b) != 0;
    }

    private void validateUsable(Voucher v, BigDecimal base, LocalDateTime now) {
        if (v.getStatus() == VoucherStatus.INACTIVE) {
            throw new AppException(VoucherErrorCode.VOUCHER_NOT_ACTIVE);
        }
        if (now.isBefore(v.getStartDate())) {
            throw new AppException(VoucherErrorCode.VOUCHER_NOT_STARTED);
        }
        if (now.isAfter(v.getEndDate())) {
            throw new AppException(VoucherErrorCode.VOUCHER_EXPIRED);
        }
        if (v.getUsedQuantity() >= v.getTotalQuantity()) {
            throw new AppException(VoucherErrorCode.VOUCHER_OUT_OF_STOCK);
        }
        if (base.compareTo(v.getMinOrderValue()) < 0) {
            throw new AppException(VoucherErrorCode.VOUCHER_MIN_ORDER_NOT_MET);
        }
    }

    private void validateRules(DiscountType type, BigDecimal value, LocalDateTime start, LocalDateTime end) {
        if (!end.isAfter(start)) {
            throw new AppException(VoucherErrorCode.VOUCHER_INVALID_DATE_RANGE);
        }
        if (type == DiscountType.PERCENT && value.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new AppException(VoucherErrorCode.VOUCHER_INVALID_DISCOUNT_VALUE);
        }
    }

    private String normalize(String code) {
        return code == null ? null : code.trim().toUpperCase();
    }

    private Voucher findOrThrow(UUID id) {
        return voucherRepository.findById(id)
                .orElseThrow(() -> new AppException(VoucherErrorCode.VOUCHER_NOT_FOUND));
    }
    private Specification<Voucher> spec(VoucherDisplayStatus status, String keyword, LocalDateTime now) {
        return (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();

            if (keyword != null && !keyword.isBlank()) {
                String like = "%" + keyword.trim().toLowerCase() + "%";
                ps.add(cb.or(
                        cb.like(cb.lower(root.get("code")), like),
                        cb.like(cb.lower(root.get("name")), like)));
            }

            if (status != null) {
                Predicate active = cb.equal(root.get("status"), VoucherStatus.ACTIVE);
                Predicate started = cb.lessThanOrEqualTo(root.<LocalDateTime>get("startDate"), now);
                Predicate notEnded = cb.greaterThanOrEqualTo(root.<LocalDateTime>get("endDate"), now);
                Predicate soldOut = cb.greaterThanOrEqualTo(
                        root.<Integer>get("usedQuantity"), root.<Integer>get("totalQuantity"));

                switch (status) {
                    case DISABLED -> ps.add(cb.equal(root.get("status"), VoucherStatus.INACTIVE));
                    case UPCOMING -> ps.add(cb.and(active,
                            cb.greaterThan(root.<LocalDateTime>get("startDate"), now)));
                    case EXPIRED -> ps.add(cb.and(active,
                            cb.lessThan(root.<LocalDateTime>get("endDate"), now)));
                    case OUT_OF_STOCK -> ps.add(cb.and(active, started, notEnded, soldOut));
                    case RUNNING -> ps.add(cb.and(active, started, notEnded, cb.not(soldOut)));
                }
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };
    }
}
