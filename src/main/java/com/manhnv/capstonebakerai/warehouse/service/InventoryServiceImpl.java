package com.manhnv.capstonebakerai.warehouse.service;

import com.manhnv.capstonebakerai.product.dtos.ComboItemRequest;
import com.manhnv.capstonebakerai.product.dtos.InventoryProduct;
import com.manhnv.capstonebakerai.product.service.ProductService;
import com.manhnv.capstonebakerai.warehouse.dto.AvailableInventoryResponse;
import com.manhnv.capstonebakerai.warehouse.dto.StockLine;
import com.manhnv.capstonebakerai.warehouse.entity.StockLot;
import com.manhnv.capstonebakerai.warehouse.enums.LotStatus;
import com.manhnv.capstonebakerai.warehouse.repository.StockLotRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class InventoryServiceImpl implements InventoryService {
    private final StockOutService stockOutService;
    private final ProductService products;
    private final StockLotRepository stockLots;

    public InventoryServiceImpl(StockOutService stockOutService, ProductService products,
                                StockLotRepository stockLots) {
        this.stockOutService = stockOutService;
        this.products = products;
        this.stockLots = stockLots;
    }

    @Override
    @Transactional(readOnly = true)
    public AvailableInventoryResponse getAvailableInventory(UUID productId) {
        StockQuantities stockQuantities = calculateAvailableQuantity(productId, new HashSet<>());
        return new AvailableInventoryResponse(productId, stockQuantities.available());
    }

    private StockQuantities calculateAvailableQuantity(UUID productId, Set<UUID> productsInPath) {
        if (!productsInPath.add(productId)) {
            throw WarehouseSupport.error("Combo composition contains a cycle");
        }

        InventoryProduct inventoryProduct = products.getInventoryProduct(productId);
        if (!inventoryProduct.combo()) {
            BigDecimal availableQuantity = stockLots
                    .findSaleableLots(productId, LotStatus.STORAGE, LocalDate.now())
                    .stream()
                    .map(StockLot::getCurrentQuantity)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            productsInPath.remove(productId);
            return new StockQuantities(availableQuantity);
        }

        List<ComboItemRequest> comboItems = products.getComboItems(productId);
        if (comboItems.isEmpty()) {
            productsInPath.remove(productId);
            return new StockQuantities(BigDecimal.ZERO);
        }

        BigDecimal possibleComboQuantity = null;
        for (ComboItemRequest comboItem : comboItems) {
            StockQuantities childQuantities = calculateAvailableQuantity(comboItem.productId(), productsInPath);
            BigDecimal possibleFromChild = childQuantities.available()
                    .divide(comboItem.quantity(), 6, RoundingMode.DOWN);
            if (possibleComboQuantity == null || possibleFromChild.compareTo(possibleComboQuantity) < 0) {
                possibleComboQuantity = possibleFromChild;
            }
        }

        productsInPath.remove(productId);
        return new StockQuantities(possibleComboQuantity == null ? BigDecimal.ZERO : possibleComboQuantity);
    }

    private record StockQuantities(BigDecimal available) {
    }

    @Override
    @Transactional
    public void deductForOrder(UUID orderId, List<StockLine> lines) {
        stockOutService.processSaleOrder(orderId, lines);
    }

    @Override
    @Transactional
    public void restockForOrder(UUID orderId) {
        stockOutService.cancelSaleOrder(orderId);
    }
}
