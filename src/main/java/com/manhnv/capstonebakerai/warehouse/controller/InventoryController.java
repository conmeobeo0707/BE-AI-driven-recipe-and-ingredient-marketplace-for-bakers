package com.manhnv.capstonebakerai.warehouse.controller;

import com.manhnv.capstonebakerai.common.response.ApiResponse;
import com.manhnv.capstonebakerai.product.service.ProductService;
import com.manhnv.capstonebakerai.warehouse.entity.StockLot;
import com.manhnv.capstonebakerai.warehouse.repository.StockLotRepository;
import com.manhnv.capstonebakerai.warehouse.service.InventoryService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;
import java.util.*;

@RestController @RequestMapping("/api/warehouse/inventory") @Validated
public class InventoryController {
    private final ProductService products; private final StockLotRepository lots;
    private final InventoryService inventory;
    public InventoryController(ProductService products, StockLotRepository lots, InventoryService inventory){this.products=products;this.lots=lots;this.inventory=inventory;}
    @GetMapping("/lots")
    public ApiResponse<List<StockLot>> lots(@RequestParam UUID productId){
        products.getInventoryProduct(productId);
        return ApiResponse.success(lots.findByProductIdOrderByExpiryDateAscCreatedAtAsc(productId));
    }
    @GetMapping("/expiring-lots")
    public ApiResponse<List<StockLot>> expiring(@RequestParam(defaultValue="30") @Min(0) @Max(3650) int withinDays){
        return ApiResponse.success(inventory.getExpiringLots(withinDays));
    }
    @GetMapping("/expired-lots")
    public ApiResponse<List<StockLot>> expired(){
        return ApiResponse.success(inventory.getExpiredLots());
    }
}
