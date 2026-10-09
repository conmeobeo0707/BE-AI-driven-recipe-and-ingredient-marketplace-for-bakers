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

@Service @Transactional
public class CategoryService {
    private final ProductCategoryRepository categories;
    public CategoryService(ProductCategoryRepository categories) { this.categories = categories; }
    public CategoryResponse create(CategoryRequest request) {
        validate(request);
        ProductCategory c = new ProductCategory(); c.setCategoryName(request.categoryName().trim());
        return response(categories.save(c));
    }
    public CategoryResponse update(UUID id, CategoryRequest request) {
        validate(request);
        ProductCategory c = categories.findById(id).orElseThrow(() -> new AppException(ProductErrorCode.NOT_FOUND, "Category was not found"));
        c.setCategoryName(request.categoryName().trim());
        return response(categories.save(c));
    }
    @Transactional(readOnly = true)
    public List<CategoryResponse> list() { return categories.findAll().stream().map(this::response).toList(); }
    public void delete(UUID id) {
        ProductCategory c = categories.findById(id).orElseThrow(() -> new AppException(ProductErrorCode.NOT_FOUND, "Category was not found"));
        try { categories.delete(c); categories.flush(); }
        catch (Exception e) { throw new AppException(ProductErrorCode.CONFLICT, "Category is in use by a product"); }
    }
    private void validate(CategoryRequest r) {
        if (r == null || r.categoryName() == null || r.categoryName().isBlank())
            throw new AppException(ProductErrorCode.INVALID_REQUEST, "Category name is required");
    }
    private CategoryResponse response(ProductCategory c) { return new CategoryResponse(c.getProductCategoryId(), c.getCategoryName(), c.getCreatedAt()); }
}
