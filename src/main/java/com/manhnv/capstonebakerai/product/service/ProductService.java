package com.manhnv.capstonebakerai.product.service;

import com.manhnv.capstonebakerai.product.dtos.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.UUID;
import java.util.Collection;
import java.util.Map;
import java.math.BigDecimal;

public interface ProductService {
    ProductResponse create(ProductUpsertRequest request);
    ProductResponse update(UUID productId, ProductUpsertRequest request);
    ProductResponse get(UUID productId);
    Page<ProductResponse> list(UUID categoryId, String brand, Pageable pageable);
    void deactivate(UUID productId);
    List<UnitPriceHistoryResponse> getUnitPriceHistory(UUID productId, UUID unitId);
    List<ComboItemRequest> setComboItems(UUID productId, List<ComboItemRequest> items);
    List<ComboItemRequest> getComboItems(UUID productId);
    java.math.BigDecimal calculateComboStock(UUID productId);
    List<InventoryProduct> lockInventoryProducts(Collection<UUID> productIds);
    InventoryProduct getInventoryProduct(UUID productId);
    InventoryUnit getActiveInventoryUnit(UUID productId, UUID unitId);
    InventoryUnit getActiveBaseInventoryUnit(UUID productId);
    void adjustInventoryStock(Map<UUID, BigDecimal> stockDeltas);
    List<ProductResponse> listLowStockProducts();
}
