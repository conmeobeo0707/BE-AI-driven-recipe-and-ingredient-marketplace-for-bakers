package com.manhnv.capstonebakerai.warehouse.controller;

import com.manhnv.capstonebakerai.common.exception.AppException;
import com.manhnv.capstonebakerai.common.response.ApiResponse;
import com.manhnv.capstonebakerai.product.entities.Product;
import com.manhnv.capstonebakerai.product.enums.ProductStatus;
import com.manhnv.capstonebakerai.product.repository.ProductRepository;
import com.manhnv.capstonebakerai.warehouse.entity.StockLot;
import com.manhnv.capstonebakerai.warehouse.enums.LotStatus;
import com.manhnv.capstonebakerai.warehouse.exception.WarehouseErrorCode;
import com.manhnv.capstonebakerai.warehouse.repository.StockLotRepository;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;
import java.time.LocalDate;
import java.util.*;

@RestController @RequestMapping("/api/warehouse/inventory") @Validated
public class InventoryController {
    private final ProductRepository products; private final StockLotRepository lots;
    public InventoryController(ProductRepository products,StockLotRepository lots){this.products=products;this.lots=lots;}
    @GetMapping("/lots")
    public ApiResponse<List<StockLot>> lots(@RequestParam UUID productId){
        if(!products.existsById(productId))throw new AppException(WarehouseErrorCode.NOT_FOUND,"Product not found");
        return ApiResponse.success(lots.findByProductIdOrderByExpiryDateAscCreatedAtAsc(productId));
    }
    @GetMapping("/low-stock")
    public ApiResponse<List<Product>> lowStock(){
        return ApiResponse.success(products.findLowStockProducts(ProductStatus.ACTIVE));
    }
    @GetMapping("/expiring-lots")
    public ApiResponse<List<StockLot>> expiring(@RequestParam(defaultValue="30") @Min(0) @Max(3650) int withinDays){
        LocalDate today=LocalDate.now();return ApiResponse.success(lots.findExpiringLots(LotStatus.STORAGE,today,today.plusDays(withinDays)));
    }
}
