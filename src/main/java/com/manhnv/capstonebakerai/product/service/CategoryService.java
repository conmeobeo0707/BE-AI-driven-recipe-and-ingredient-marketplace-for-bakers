package com.manhnv.capstonebakerai.product.service;

import com.manhnv.capstonebakerai.product.dtos.CategoryRequest;
import com.manhnv.capstonebakerai.product.dtos.CategoryResponse;
import java.util.List;
import java.util.UUID;

public interface CategoryService {
    CategoryResponse create(CategoryRequest request);
    CategoryResponse update(UUID categoryId, CategoryRequest request);
    List<CategoryResponse> list();
    void delete(UUID categoryId);
}
