package com.manhnv.capstonebakerai.product.controller;

import com.manhnv.capstonebakerai.product.dtos.*;
import com.manhnv.capstonebakerai.product.service.ProductService;
import com.manhnv.capstonebakerai.common.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/products")
public class ProductController {
    private final ProductService service;
    public ProductController(ProductService service) { this.service = service; }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ProductResponse> create(@Valid @RequestBody ProductUpsertRequest request) { return ApiResponse.success(service.create(request)); }
    @PutMapping("/{productId}")
    public ApiResponse<ProductResponse> update(@PathVariable UUID productId, @Valid @RequestBody ProductUpsertRequest request) { return ApiResponse.success(service.update(productId, request)); }
    @GetMapping("/{productId}")
    public ApiResponse<ProductResponse> get(@PathVariable UUID productId) { return ApiResponse.success(service.get(productId)); }
    @GetMapping
    public ApiResponse<Page<ProductResponse>> list(@RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) String brand, Pageable pageable) { return ApiResponse.success(service.list(categoryId, brand, pageable)); }
    @DeleteMapping("/{productId}")
    public ApiResponse<Void> deactivate(@PathVariable UUID productId) { service.deactivate(productId); return ApiResponse.success(null); }
    @GetMapping("/{productId}/units/{unitId}/price-history")
    public ApiResponse<List<UnitPriceHistoryResponse>> unitPriceHistory(@PathVariable UUID productId, @PathVariable UUID unitId) {
        return ApiResponse.success(service.getUnitPriceHistory(productId, unitId));
    }
    @PutMapping("/{productId}/combo-items")
    public ApiResponse<List<ComboItemRequest>> setComboItems(@PathVariable UUID productId,
            @Valid @RequestBody List<@Valid ComboItemRequest> items) { return ApiResponse.success(service.setComboItems(productId, items)); }
    @GetMapping("/{productId}/combo-stock")
    public ApiResponse<BigDecimal> comboStock(@PathVariable UUID productId) { return ApiResponse.success(service.calculateComboStock(productId)); }
}
