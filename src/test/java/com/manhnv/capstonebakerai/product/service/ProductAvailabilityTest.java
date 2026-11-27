package com.manhnv.capstonebakerai.product.service;

import com.manhnv.capstonebakerai.common.exception.AppException;
import com.manhnv.capstonebakerai.product.entities.ChildProduct;
import com.manhnv.capstonebakerai.product.entities.Product;
import com.manhnv.capstonebakerai.product.entities.ProductUnit;
import com.manhnv.capstonebakerai.product.enums.ProductStatus;
import com.manhnv.capstonebakerai.product.repository.*;
import com.manhnv.capstonebakerai.warehouse.repository.StockLotRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class ProductAvailabilityTest {
    @Test
    void statusRequiresEnoughStockForOneEnabledSaleUnit() {
        var products = mock(ProductRepository.class);
        var units = mock(ProductUnitRepository.class);
        var lots = mock(StockLotRepository.class);
        var service = new ProductServiceImpl(products, units, mock(ChildProductRepository.class),
                mock(UnitPriceHistoryRepository.class), mock(ProductCategoryRepository.class), lots);
        UUID productId = UUID.randomUUID();
        var product = new Product();
        product.setProductId(productId);
        var bag = new ProductUnit();
        bag.setConversionFactor(new BigDecimal("5"));
        bag.setWeightGram(5000);
        var smallPack = new ProductUnit();
        smallPack.setConversionFactor(BigDecimal.ONE);
        smallPack.setWeightGram(1000);
        smallPack.setSale(false);

        when(products.findById(productId)).thenReturn(Optional.of(product));
        when(units.findByProductId(productId)).thenReturn(List.of(bag, smallPack));
        when(lots.sumSaleableQuantity(eq(productId), any(), any())).thenReturn(new BigDecimal("2"));

        assertEquals(ProductStatus.INACTIVE, service.get(productId).status());

        smallPack.setSale(true);
        assertEquals(ProductStatus.ACTIVE, service.get(productId).status());

        product.setStatus(ProductStatus.DISCONTINUE);
        assertEquals(ProductStatus.DISCONTINUE, service.get(productId).status());
    }

    @Test
    void nestedComboSharesOneStockBalanceAcrossBranches() {
        var products = mock(ProductRepository.class);
        var units = mock(ProductUnitRepository.class);
        var components = mock(ChildProductRepository.class);
        var lots = mock(StockLotRepository.class);
        var service = new ProductServiceImpl(products, units, components,
                mock(UnitPriceHistoryRepository.class), mock(ProductCategoryRepository.class), lots);

        Product bundle = product(true, BigDecimal.ZERO);
        Product first = product(true, BigDecimal.ZERO);
        Product second = product(true, BigDecimal.ZERO);
        Product butter = product(false, BigDecimal.ONE);
        for (Product item : List.of(bundle, first, second, butter)) {
            when(products.findById(item.getProductId())).thenReturn(Optional.of(item));
        }
        when(components.findByParentProductId(bundle.getProductId())).thenReturn(List.of(part(first), part(second)));
        when(components.findByParentProductId(first.getProductId())).thenReturn(List.of(part(butter)));
        when(components.findByParentProductId(second.getProductId())).thenReturn(List.of(part(butter)));

        var unit = new ProductUnit();
        unit.setConversionFactor(BigDecimal.ONE);
        unit.setWeightGram(100);
        when(units.findByProductId(bundle.getProductId())).thenReturn(List.of(unit));

        when(lots.sumSaleableQuantity(eq(butter.getProductId()), any(), any())).thenReturn(BigDecimal.ONE);
        assertEquals(Map.of(butter.getProductId(), new BigDecimal("2")), service.getSaleRequirements(bundle.getProductId()));
        var outOfStock = service.get(bundle.getProductId());
        assertEquals(ProductStatus.INACTIVE, outOfStock.status());
        assertEquals(BigDecimal.ZERO, outOfStock.saleableQuantity());
        assertEquals(BigDecimal.ZERO, service.calculateComboStock(bundle.getProductId()));

        var page = PageRequest.of(0, 10);
        when(products.findAll(org.mockito.ArgumentMatchers.<Specification<Product>>any(), any(Sort.class)))
                .thenReturn(List.of(bundle));
        assertEquals(1, service.list(null, null, null, null, ProductStatus.INACTIVE, page).getTotalElements());
        assertEquals(0, service.list(null, null, null, null, ProductStatus.ACTIVE, page).getTotalElements());

        butter.setStockQuantity(new BigDecimal("2"));
        when(lots.sumSaleableQuantity(eq(butter.getProductId()), any(), any())).thenReturn(new BigDecimal("2"));
        var inStock = service.get(bundle.getProductId());
        assertEquals(ProductStatus.ACTIVE, inStock.status());
        assertEquals(0, BigDecimal.ONE.compareTo(inStock.saleableQuantity()));

        second.setStatus(ProductStatus.DISCONTINUE);
        assertEquals(BigDecimal.ZERO, service.calculateComboStock(bundle.getProductId()));
        assertThrows(AppException.class, () -> service.getSaleRequirements(bundle.getProductId()));
    }

    private Product product(boolean combo, BigDecimal stock) {
        var product = new Product();
        product.setProductId(UUID.randomUUID());
        product.setCombo(combo);
        product.setStockQuantity(stock);
        return product;
    }

    private ChildProduct part(Product product) {
        var part = new ChildProduct();
        part.setChildrenProductId(product.getProductId());
        part.setQuantity(BigDecimal.ONE);
        return part;
    }
}
