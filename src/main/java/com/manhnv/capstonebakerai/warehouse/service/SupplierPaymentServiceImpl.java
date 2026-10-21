package com.manhnv.capstonebakerai.warehouse.service;

import com.manhnv.capstonebakerai.common.exception.AppException;
import com.manhnv.capstonebakerai.common.security.SecurityUtils;
import com.manhnv.capstonebakerai.warehouse.dto.SupplierPaymentCreateRequest;
import com.manhnv.capstonebakerai.warehouse.dto.SupplierPaymentDetailResponse;
import com.manhnv.capstonebakerai.warehouse.dto.SupplierPaymentResponse;
import com.manhnv.capstonebakerai.warehouse.entity.StockInNote;
import com.manhnv.capstonebakerai.warehouse.entity.SupplierPayment;
import com.manhnv.capstonebakerai.warehouse.entity.SupplierPaymentDetail;
import com.manhnv.capstonebakerai.warehouse.enums.NoteStatus;
import com.manhnv.capstonebakerai.warehouse.enums.PaymentStatus;
import com.manhnv.capstonebakerai.warehouse.exception.WarehouseErrorCode;
import com.manhnv.capstonebakerai.warehouse.repository.StockInNoteRepository;
import com.manhnv.capstonebakerai.warehouse.repository.SupplierPaymentDetailRepository;
import com.manhnv.capstonebakerai.warehouse.repository.SupplierPaymentRepository;
import com.manhnv.capstonebakerai.warehouse.repository.SupplierRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class SupplierPaymentServiceImpl implements SupplierPaymentService {
    private final SupplierRepository suppliers;
    private final SupplierPaymentRepository payments;
    private final SupplierPaymentDetailRepository paymentDetails;
    private final StockInNoteRepository stockInNotes;

    public SupplierPaymentServiceImpl(SupplierRepository suppliers, SupplierPaymentRepository payments,
            SupplierPaymentDetailRepository paymentDetails, StockInNoteRepository stockInNotes) {
        this.suppliers = suppliers;
        this.payments = payments;
        this.paymentDetails = paymentDetails;
        this.stockInNotes = stockInNotes;
    }

    @Override
    @Transactional
    public SupplierPaymentDetailResponse create(UUID supplierId, SupplierPaymentCreateRequest request) {
        if (!suppliers.existsById(supplierId)) {
            throw new AppException(WarehouseErrorCode.NOT_FOUND, "Supplier not found");
        }

        Set<UUID> requestedNoteIds = new HashSet<>();
        BigDecimal allocatedTotal = BigDecimal.ZERO;
        for (SupplierPaymentCreateRequest.Allocation allocation : request.details()) {
            if (!requestedNoteIds.add(allocation.stockInNoteId())) {
                throw new AppException(WarehouseErrorCode.INVALID_REQUEST,
                        "A stock-in note can only appear once in a payment");
            }
            allocatedTotal = allocatedTotal.add(allocation.amount());
        }
        if (allocatedTotal.compareTo(request.amount()) != 0) {
            throw new AppException(WarehouseErrorCode.INVALID_REQUEST,
                    "Payment amount must equal the sum of its allocation details");
        }

        List<StockInNote> lockedNotes = stockInNotes.lockBySupplierAndNoteIdsAndStatus(
                supplierId, requestedNoteIds, NoteStatus.COMPLETED);
        if (lockedNotes.size() != requestedNoteIds.size()) {
            throw new AppException(WarehouseErrorCode.NOT_FOUND,
                    "One or more completed stock-in notes do not belong to this supplier");
        }

        Map<UUID, StockInNote> notesById = new HashMap<>();
        lockedNotes.forEach(stockInNote -> notesById.put(stockInNote.getNoteId(), stockInNote));
        for (SupplierPaymentCreateRequest.Allocation allocation : request.details()) {
            StockInNote stockInNote = notesById.get(allocation.stockInNoteId());
            if (allocation.amount().compareTo(stockInNote.getDebtAmount()) > 0) {
                throw new AppException(WarehouseErrorCode.CONFLICT,
                        "Allocation exceeds the remaining debt for stock-in note " + stockInNote.getCode());
            }
        }

        SupplierPayment supplierPayment = new SupplierPayment();
        supplierPayment.setCode(generateCode());
        supplierPayment.setSupplierId(supplierId);
        supplierPayment.setAmount(request.amount());
        supplierPayment.setCreatedBy(SecurityUtils.getCurrentUserId());
        supplierPayment.setCreatedAt(LocalDateTime.now());
        SupplierPayment savedPayment = payments.save(supplierPayment);

        List<SupplierPaymentDetail> savedDetails = new ArrayList<>();
        for (SupplierPaymentCreateRequest.Allocation allocation : request.details()) {
            StockInNote stockInNote = notesById.get(allocation.stockInNoteId());
            BigDecimal allocatedAmount = allocation.amount();
            stockInNote.setPaidAmount(stockInNote.getPaidAmount().add(allocatedAmount));
            stockInNote.setDebtAmount(stockInNote.getDebtAmount().subtract(allocatedAmount));
            stockInNote.setPaymentStatus(stockInNote.getDebtAmount().signum() == 0
                    ? PaymentStatus.PAID : PaymentStatus.PARTIAL);
            stockInNote.setUpdatedAt(LocalDateTime.now());

            SupplierPaymentDetail paymentDetail = new SupplierPaymentDetail();
            paymentDetail.setSupplierPaymentsId(savedPayment.getId());
            paymentDetail.setStockInNoteId(stockInNote.getNoteId());
            paymentDetail.setAmount(allocatedAmount);
            savedDetails.add(paymentDetail);
        }
        stockInNotes.saveAll(lockedNotes);
        List<SupplierPaymentDetail> savedPaymentDetails = paymentDetails.saveAll(savedDetails);
        return toDetailResponse(savedPayment, savedPaymentDetails, notesById);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SupplierPaymentResponse> list(UUID supplierId, Pageable pageable) {
        if (!suppliers.existsById(supplierId)) {
            throw new AppException(WarehouseErrorCode.NOT_FOUND, "Supplier not found");
        }
        return payments.findBySupplierId(supplierId, pageable).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public SupplierPaymentDetailResponse get(UUID supplierId, UUID paymentId) {
        SupplierPayment supplierPayment = payments.findByIdAndSupplierId(paymentId, supplierId)
                .orElseThrow(() -> new AppException(WarehouseErrorCode.NOT_FOUND, "Supplier payment not found"));
        List<SupplierPaymentDetail> details = paymentDetails.findBySupplierPaymentsIdOrderById(paymentId);
        Map<UUID, StockInNote> notesById = new HashMap<>();
        stockInNotes.findAllById(details.stream().map(SupplierPaymentDetail::getStockInNoteId).toList())
                .forEach(stockInNote -> notesById.put(stockInNote.getNoteId(), stockInNote));
        return toDetailResponse(supplierPayment, details, notesById);
    }

    private SupplierPaymentDetailResponse toDetailResponse(SupplierPayment supplierPayment,
            List<SupplierPaymentDetail> details, Map<UUID, StockInNote> notesById) {
        List<SupplierPaymentDetailResponse.Allocation> allocations = details.stream()
                .map(detail -> {
                    StockInNote stockInNote = notesById.get(detail.getStockInNoteId());
                    return new SupplierPaymentDetailResponse.Allocation(detail.getId(), detail.getStockInNoteId(),
                            stockInNote == null ? null : stockInNote.getCode(), detail.getAmount());
                }).toList();
        return new SupplierPaymentDetailResponse(toResponse(supplierPayment), allocations);
    }

    private SupplierPaymentResponse toResponse(SupplierPayment supplierPayment) {
        return new SupplierPaymentResponse(supplierPayment.getId(), supplierPayment.getCode(),
                supplierPayment.getSupplierId(), supplierPayment.getAmount(), supplierPayment.getCreatedAt(),
                supplierPayment.getCreatedBy());
    }

    private String generateCode() {
        String code;
        do {
            code = "SP-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase(Locale.ROOT);
        } while (payments.existsByCode(code));
        return code;
    }
}
