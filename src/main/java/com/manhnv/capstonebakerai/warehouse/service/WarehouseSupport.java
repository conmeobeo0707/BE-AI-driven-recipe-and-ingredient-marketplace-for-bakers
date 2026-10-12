package com.manhnv.capstonebakerai.warehouse.service;

import com.manhnv.capstonebakerai.common.exception.AppException;
import com.manhnv.capstonebakerai.product.entities.Product;
import com.manhnv.capstonebakerai.product.entities.ProductUnit;
import com.manhnv.capstonebakerai.product.entities.UnitStatus;
import com.manhnv.capstonebakerai.product.repository.ProductRepository;
import com.manhnv.capstonebakerai.product.repository.ProductUnitRepository;
import com.manhnv.capstonebakerai.warehouse.exception.WarehouseErrorCode;
import com.manhnv.capstonebakerai.warehouse.repository.StocktakeItemRepository;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Component
class WarehouseSupport {
    private final ProductRepository products;
    private final ProductUnitRepository units;
    private final StocktakeItemRepository stocktakes;

    WarehouseSupport(ProductRepository products, ProductUnitRepository units, StocktakeItemRepository stocktakes) {
        this.products = products; this.units = units; this.stocktakes = stocktakes;
    }
    Product product(UUID id) {
        Product p = products.findById(id).orElseThrow(() -> error("Product not found: " + id));
        if (p.isCombo()) throw error("Inventory movements are not supported for combo products");
        return p;
    }
    ProductUnit unit(UUID productId, UUID unitId) {
        ProductUnit u = units.findByUnitIdAndProductId(unitId, productId)
                .orElseThrow(() -> error("Unit does not belong to product: " + unitId));
        if (u.getStatus() != UnitStatus.ACTIVE || u.getConversionFactor() == null || u.getConversionFactor().signum() <= 0)
            throw error("Unit is inactive or has an invalid conversion factor");
        return u;
    }
    ProductUnit baseUnit(UUID productId) {
        return units.findByProductIdAndBaseTrueAndStatus(productId,UnitStatus.ACTIVE)
                .orElseThrow(() -> error("Product has no active base unit"));
    }
    List<Product> lockProducts(Collection<UUID> ids) {
        List<UUID> sorted = ids.stream().distinct().sorted().toList();
        List<Product> locked = products.lockProducts(sorted);
        if (locked.size() != sorted.size()) throw error("One or more products were not found");
        return locked;
    }
    void ensureNoDraftStocktake(Collection<UUID> ids) {
        for (UUID id : ids) if (stocktakes.hasDraftStocktakeForProduct(id))
            throw new AppException(WarehouseErrorCode.CONFLICT, "Product is locked by an active stocktake: " + id);
    }
    void ensureLotsNotInDraftStocktake(Collection<UUID> lotIds) {
        for(UUID lotId:lotIds) if(stocktakes.hasDraftStocktakeForLot(lotId))
            throw new AppException(WarehouseErrorCode.CONFLICT,"Lot is locked by an active stocktake: "+lotId);
    }
    boolean isLotInDraftStocktake(UUID lotId) { return stocktakes.hasDraftStocktakeForLot(lotId); }
    static AppException error(String message) { return new AppException(WarehouseErrorCode.INVALID_REQUEST, message); }
    static BigDecimal money(BigDecimal value) { return value.setScale(2, RoundingMode.HALF_UP); }
    static String code(String prefix) { return prefix + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT); }
}
