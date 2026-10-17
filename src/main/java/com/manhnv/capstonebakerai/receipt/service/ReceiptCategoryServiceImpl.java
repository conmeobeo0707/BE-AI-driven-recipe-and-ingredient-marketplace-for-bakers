package com.manhnv.capstonebakerai.receipt.service;

import com.manhnv.capstonebakerai.common.exception.AppException;
import com.manhnv.capstonebakerai.receipt.dto.request.ReceiptCategoryRequest;
import com.manhnv.capstonebakerai.receipt.dto.response.ReceiptCategoryResponse;
import com.manhnv.capstonebakerai.receipt.entity.ReceiptCategory;
import com.manhnv.capstonebakerai.receipt.enums.ReceiptCategoryType;
import com.manhnv.capstonebakerai.receipt.exception.ReceiptCategoryErrorCode;
import com.manhnv.capstonebakerai.receipt.mapper.ReceiptCategoryMapper;
import com.manhnv.capstonebakerai.receipt.repository.ReceiptCategoryRepository;
import com.manhnv.capstonebakerai.receipt.repository.ReceiptVoucherRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ReceiptCategoryServiceImpl implements ReceiptCategoryService {

    ReceiptCategoryRepository categoryRepository;
    ReceiptVoucherRepository voucherRepository;
    ReceiptCategoryMapper mapper;


    @Override
    @Transactional
    public ReceiptCategoryResponse create(ReceiptCategoryRequest request) {
        if (categoryRepository.existsByNameIgnoreCase(request.name().trim())) {
            throw new AppException(ReceiptCategoryErrorCode.RECEIPT_CATEGORY_NAME_EXISTED);
        }

        ReceiptCategory category = ReceiptCategory.builder()
                .name(request.name().trim())
                .type(request.type())
                .build();

        return mapper.toResponse(categoryRepository.save(category));
    }

    @Override
    @Transactional
    public ReceiptCategoryResponse update(UUID id, ReceiptCategoryRequest request) {
        ReceiptCategory category = findOrThrow(id);
        if (categoryRepository.existsByNameIgnoreCaseAndIdNot(request.name().trim(), id)) {
            throw new AppException(ReceiptCategoryErrorCode.RECEIPT_CATEGORY_NAME_EXISTED);
        }
        category.setName(request.name().trim());
        category.setType(request.type());
        return mapper.toResponse(categoryRepository.save(category));
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        ReceiptCategory category = findOrThrow(id);
        if (voucherRepository.existsByReceiptCategoryId(id)) {
            throw new AppException(ReceiptCategoryErrorCode.RECEIPT_CATEGORY_IN_USE);
        }
        categoryRepository.delete(category);
    }

    @Override
    @Transactional(readOnly = true)
    public ReceiptCategoryResponse getById(UUID id) {
        return mapper.toResponse(findOrThrow(id));
    }

    @Override
    public List<ReceiptCategoryResponse> getAll() {
        return categoryRepository.findAll().stream().map(mapper::toResponse).toList();
    }

    private ReceiptCategory findOrThrow(UUID id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new AppException(ReceiptCategoryErrorCode.RECEIPT_CATEGORY_NOT_FOUND));
    }
}
