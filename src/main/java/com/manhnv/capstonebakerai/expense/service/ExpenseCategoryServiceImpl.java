package com.manhnv.capstonebakerai.expense.service;

import com.manhnv.capstonebakerai.common.exception.AppException;
import com.manhnv.capstonebakerai.expense.dto.request.ExpenseCategoryRequest;
import com.manhnv.capstonebakerai.expense.dto.response.ExpenseCategoryResponse;
import com.manhnv.capstonebakerai.expense.entity.ExpenseCategory;
import com.manhnv.capstonebakerai.expense.exception.ExpenseCategoryErrorCode;
import com.manhnv.capstonebakerai.expense.mapper.ExpenseCategoryMapper;
import com.manhnv.capstonebakerai.expense.repository.ExpenseCategoryRepository;
import com.manhnv.capstonebakerai.expense.repository.ExpenseVoucherRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ExpenseCategoryServiceImpl implements ExpenseCategoryService {

    ExpenseCategoryRepository categoryRepository;
    ExpenseVoucherRepository voucherRepository;
    ExpenseCategoryMapper mapper;

    @Override
    @Transactional
    public ExpenseCategoryResponse create(ExpenseCategoryRequest request) {
        if (categoryRepository.existsByNameIgnoreCase(request.name())) {
            throw new AppException(ExpenseCategoryErrorCode.EXPENSE_CATEGORY_NAME_EXISTED);
        }
        ExpenseCategory entity = ExpenseCategory.builder()
                .name(request.name().trim())
                .type(request.type())
                .createdAt(LocalDateTime.now())
                .build();

        return mapper.toResponse(categoryRepository.save(entity));
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public ExpenseCategoryResponse update(UUID expenseCategoryId, ExpenseCategoryRequest request) {
        ExpenseCategory entity = findOrThrow(expenseCategoryId);
        if (categoryRepository.existsByNameIgnoreCase(request.name())) {
            throw new AppException(ExpenseCategoryErrorCode.EXPENSE_CATEGORY_NAME_EXISTED);
        }
        entity.setName(request.name().trim());
        entity.setType(request.type());
        return mapper.toResponse(categoryRepository.save(entity));
    }

    @Override
    @Transactional
    public void delete(UUID expenseCategoryId) {
        ExpenseCategory entity = findOrThrow(expenseCategoryId);
        if (voucherRepository.existsByExpenseCategoryId(expenseCategoryId)) {
            throw new AppException(ExpenseCategoryErrorCode.EXPENSE_CATEGORY_IN_USE);
        }
        categoryRepository.delete(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExpenseCategoryResponse> getAll() {
        return categoryRepository.findAll().stream().map(mapper::toResponse).toList();
    }

    private ExpenseCategory findOrThrow(UUID id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new AppException(ExpenseCategoryErrorCode.EXPENSE_CATEGORY_NOT_FOUND));
    }
}
