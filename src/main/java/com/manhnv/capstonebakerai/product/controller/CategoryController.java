package com.manhnv.capstonebakerai.product.controller;

import com.manhnv.capstonebakerai.product.dtos.CategoryRequest;
import com.manhnv.capstonebakerai.product.dtos.CategoryResponse;
import com.manhnv.capstonebakerai.product.service.CategoryService;
import com.manhnv.capstonebakerai.common.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController @RequestMapping("/api/product-categories")
public class CategoryController {
    private final CategoryService service;
    public CategoryController(CategoryService service) { this.service = service; }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CategoryResponse> create(@Valid @RequestBody CategoryRequest request) { return ApiResponse.success(service.create(request)); }
    @PutMapping("/{id}")
    public ApiResponse<CategoryResponse> update(@PathVariable UUID id, @Valid @RequestBody CategoryRequest request) { return ApiResponse.success(service.update(id, request)); }
    @GetMapping public ApiResponse<List<CategoryResponse>> list() { return ApiResponse.success(service.list()); }
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable UUID id) { service.delete(id); return ApiResponse.success(null); }
}
