package com.manhnv.capstonebakerai.receipt.service;

import com.manhnv.capstonebakerai.receipt.dto.request.ReceiptCategoryRequest;
import com.manhnv.capstonebakerai.receipt.dto.response.ReceiptCategoryResponse;
import com.manhnv.capstonebakerai.receipt.enums.ReceiptCategoryType;

import java.util.List;
import java.util.UUID;

public interface ReceiptCategoryService {
    ReceiptCategoryResponse create(ReceiptCategoryRequest request);
    ReceiptCategoryResponse update(UUID id, ReceiptCategoryRequest request);
    void delete(UUID id);
    ReceiptCategoryResponse getById(UUID id);
    List<ReceiptCategoryResponse> getAll();
}
