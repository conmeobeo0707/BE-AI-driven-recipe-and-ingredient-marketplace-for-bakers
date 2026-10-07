package com.manhnv.capstonebakerai.expense.service;


import com.manhnv.capstonebakerai.expense.dto.request.ExpenseCategoryRequest;
import com.manhnv.capstonebakerai.expense.dto.response.ExpenseCategoryResponse;

import java.util.List;
import java.util.UUID;

public interface ExpenseCategoryService {
    ExpenseCategoryResponse create(ExpenseCategoryRequest request);
    ExpenseCategoryResponse update(UUID expenseCategoryId, ExpenseCategoryRequest request);
    void delete(UUID expenseCategoryId);
    List<ExpenseCategoryResponse> getAll();
}
