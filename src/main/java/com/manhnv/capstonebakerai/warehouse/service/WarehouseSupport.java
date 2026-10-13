package com.manhnv.capstonebakerai.warehouse.service;

import com.manhnv.capstonebakerai.common.exception.AppException;
import com.manhnv.capstonebakerai.product.dtos.InventoryProduct;
import com.manhnv.capstonebakerai.product.dtos.InventoryUnit;
import com.manhnv.capstonebakerai.product.service.ProductService;
import com.manhnv.capstonebakerai.warehouse.exception.WarehouseErrorCode;
import com.manhnv.capstonebakerai.warehouse.repository.StocktakeItemRepository;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
class WarehouseSupport {
    private final ProductService products;
    private final StocktakeItemRepository stocktakes;

    WarehouseSupport(ProductService products, StocktakeItemRepository stocktakes) {
        this.products = products; this.stocktakes = stocktakes;
    }
    InventoryProduct product(UUID id) {
        InventoryProduct product=products.getInventoryProduct(id);
        if (product.combo()) throw error("Inventory movements are not supported for combo products");
        return product;
    }
    InventoryUnit unit(UUID productId, UUID unitId) {
        InventoryUnit inventoryUnit=products.getActiveInventoryUnit(productId,unitId);
        if (inventoryUnit.conversionFactor() == null || inventoryUnit.conversionFactor().signum() <= 0)
            throw error("Unit is inactive or has an invalid conversion factor");
        return inventoryUnit;
    }
    InventoryUnit baseUnit(UUID productId) {
        return products.getActiveBaseInventoryUnit(productId);
    }
    List<InventoryProduct> lockProducts(Collection<UUID> ids) {
        List<InventoryProduct> locked=products.lockInventoryProducts(ids);
        if(locked.stream().anyMatch(InventoryProduct::combo))throw error("Inventory movements are not supported for combo products");
        return locked;
    }
    void adjustStock(Map<UUID, java.math.BigDecimal> deltas) { products.adjustInventoryStock(deltas); }
    void ensureLotsNotInDraftStocktake(Collection<UUID> lotIds) {
        for(UUID lotId:lotIds) if(stocktakes.hasDraftStocktakeForLot(lotId))
            throw new AppException(WarehouseErrorCode.CONFLICT,"Lot is locked by an active stocktake: "+lotId);
    }
    boolean isLotInDraftStocktake(UUID lotId) { return stocktakes.hasDraftStocktakeForLot(lotId); }
    static AppException error(String message) { return new AppException(WarehouseErrorCode.INVALID_REQUEST, message); }
    static java.math.BigDecimal money(java.math.BigDecimal value) { return value.setScale(2, java.math.RoundingMode.HALF_UP); }
    static String code(String prefix) { return prefix + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT); }
}
