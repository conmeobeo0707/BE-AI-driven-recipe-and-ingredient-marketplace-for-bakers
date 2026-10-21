package com.manhnv.capstonebakerai.product.service;

import com.manhnv.capstonebakerai.common.exception.AppException;
import com.manhnv.capstonebakerai.product.dtos.CategoryRequest;
import com.manhnv.capstonebakerai.product.dtos.CategoryResponse;
import com.manhnv.capstonebakerai.product.entities.ProductCategory;
import com.manhnv.capstonebakerai.product.exception.ProductErrorCode;
import com.manhnv.capstonebakerai.product.repository.ProductCategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class CategoryServiceImpl implements CategoryService {
    private final ProductCategoryRepository categories;

    public CategoryServiceImpl(ProductCategoryRepository categories) {
        this.categories = categories;
    }

    @Override
    public CategoryResponse create(CategoryRequest request) {
        validate(request);
        ProductCategory category = new ProductCategory();
        category.setCategoryName(request.categoryName().trim());
        return response(categories.save(category));
    }

    @Override
    public CategoryResponse update(UUID categoryId, CategoryRequest request) {
        validate(request);
        ProductCategory category = categories.findById(categoryId)
                .orElseThrow(() -> new AppException(ProductErrorCode.NOT_FOUND, "Category was not found"));
        category.setCategoryName(request.categoryName().trim());
        return response(categories.save(category));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> list() {
        return categories.findAll().stream().map(this::response).toList();
    }

    @Override
    public void delete(UUID categoryId) {
        ProductCategory category = categories.findById(categoryId)
                .orElseThrow(() -> new AppException(ProductErrorCode.NOT_FOUND, "Category was not found"));
        try {
            categories.delete(category);
            categories.flush();
        } catch (Exception exception) {
            throw new AppException(ProductErrorCode.CONFLICT, "Category is in use by a product");
        }
    }

    private void validate(CategoryRequest request) {
        if (request == null || request.categoryName() == null || request.categoryName().isBlank()) {
            throw new AppException(ProductErrorCode.INVALID_REQUEST, "Category name is required");
        }
    }

    private CategoryResponse response(ProductCategory category) {
        return new CategoryResponse(category.getProductCategoryId(), category.getCategoryName(), category.getCreatedAt());
    }
}
