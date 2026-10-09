package com.manhnv.capstonebakerai.product.controller;

import com.manhnv.capstonebakerai.product.dtos.*;
import com.manhnv.capstonebakerai.product.service.ProductService;
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
    public ProductResponse create(@RequestBody ProductUpsertRequest request) { return service.create(request); }
    @PutMapping("/{productId}")
    public ProductResponse update(@PathVariable UUID productId, @RequestBody ProductUpsertRequest request) { return service.update(productId, request); }
    @GetMapping("/{productId}")
    public ProductResponse get(@PathVariable UUID productId) { return service.get(productId); }
    @GetMapping
    public Page<ProductResponse> list(@RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) String brand, Pageable pageable) { return service.list(categoryId, brand, pageable); }
    @DeleteMapping("/{productId}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivate(@PathVariable UUID productId) { service.deactivate(productId); }
    @PutMapping("/{productId}/units")
    public List<ProductUnitResponse> manageUnits(@PathVariable UUID productId,
            @RequestBody List<ProductUnitRequest> units) { return service.manageUnits(productId, units); }
    @PatchMapping("/{productId}/units/{unitId}/price")
    public ProductUnitResponse updateSellingPrice(@PathVariable UUID productId, @PathVariable UUID unitId,
            @RequestBody SellingPriceRequest request) {
        return service.updateSellingPrice(productId, unitId, request.sellingPrice());
    }
    @GetMapping("/{productId}/units/{unitId}/price-history")
    public List<UnitPriceHistoryResponse> unitPriceHistory(@PathVariable UUID productId, @PathVariable UUID unitId) {
        return service.getUnitPriceHistory(productId, unitId);
    }
    @PutMapping("/{productId}/combo-items")
    public List<ComboItemRequest> setComboItems(@PathVariable UUID productId,
            @RequestBody List<ComboItemRequest> items) { return service.setComboItems(productId, items); }
    @GetMapping("/{productId}/combo-stock")
    public BigDecimal comboStock(@PathVariable UUID productId) { return service.calculateComboStock(productId); }
}
