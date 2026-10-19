package com.manhnv.capstonebakerai.expense.controller;

import com.manhnv.capstonebakerai.common.response.ApiResponse;
import com.manhnv.capstonebakerai.expense.dto.request.ExpenseCategoryRequest;
import com.manhnv.capstonebakerai.expense.dto.response.ExpenseCategoryResponse;
import com.manhnv.capstonebakerai.expense.service.ExpenseCategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/expense-categories")
@RequiredArgsConstructor
@Tag(name = "Danh mục chi", description = "Các API liên quan đến quản lý danh mục chi, yêu cầu quyền MANAGER, STAFF")
public class ExpenseCategoryController {

    private final ExpenseCategoryService service;

    @Operation(
            summary = "API tạo danh mục chi",
            description =  "Cần truyền name và type(SALARY, RENT, MARKETING, SHIPPING, UTILITY, OTHER")
    @PostMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<ExpenseCategoryResponse>> create(@Valid @RequestBody ExpenseCategoryRequest request) {
        return ResponseEntity.ok(ApiResponse.success(service.create(request)));
    }

    @Operation(
            summary = "Update danh mục chi",
            description =  "Cần truyền name và type(SALARY, RENT, MARKETING, SHIPPING, UTILITY, OTHER")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<ExpenseCategoryResponse>> update(@PathVariable UUID id, @Valid @RequestBody ExpenseCategoryRequest request) {
        return ResponseEntity.ok(ApiResponse.success(service.update(id, request)));
    }

    @Operation(
            summary = "Xóa danh mục chi")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id){
        service.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @Operation(summary = "Lấy danh sách các danh mục chi")
    @GetMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<List<ExpenseCategoryResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(service.getAll()));
    }
}
