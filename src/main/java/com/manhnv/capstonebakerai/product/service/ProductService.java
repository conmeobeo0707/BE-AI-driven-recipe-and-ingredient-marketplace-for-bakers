package com.manhnv.capstonebakerai.product.service;

import com.manhnv.capstonebakerai.product.dtos.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface ProductService {
    ProductResponse create(ProductUpsertRequest request);
    ProductResponse update(UUID productId, ProductUpsertRequest request);
    ProductResponse get(UUID productId);
    Page<ProductResponse> list(UUID categoryId, String brand, Pageable pageable);
    void deactivate(UUID productId);
    List<ProductUnitResponse> manageUnits(UUID productId, List<ProductUnitRequest> units);
    ProductUnitResponse updateSellingPrice(UUID productId, UUID unitId, BigDecimal sellingPrice);
    List<UnitPriceHistoryResponse> getUnitPriceHistory(UUID productId, UUID unitId);
    List<ComboItemRequest> setComboItems(UUID productId, List<ComboItemRequest> items);
    BigDecimal calculateComboStock(UUID productId);
}
