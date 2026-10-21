package com.manhnv.capstonebakerai.warehouse.controller;

import com.manhnv.capstonebakerai.common.response.ApiResponse;
import com.manhnv.capstonebakerai.product.dtos.ProductResponse;
import com.manhnv.capstonebakerai.product.service.ProductService;
import com.manhnv.capstonebakerai.warehouse.entity.StockLot;
import com.manhnv.capstonebakerai.warehouse.dto.AvailableInventoryResponse;
import com.manhnv.capstonebakerai.warehouse.enums.LotStatus;
import com.manhnv.capstonebakerai.warehouse.repository.StockLotRepository;
import com.manhnv.capstonebakerai.warehouse.service.StockReservationService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;
import java.time.LocalDate;
import java.util.*;

@RestController @RequestMapping("/api/warehouse/inventory") @Validated
public class InventoryController {
    private final ProductService products; private final StockLotRepository lots;
    private final StockReservationService reservations;
    public InventoryController(ProductService products, StockLotRepository lots, StockReservationService reservations){this.products=products;this.lots=lots;this.reservations=reservations;}
    @GetMapping("/available")
    public ApiResponse<AvailableInventoryResponse> available(@RequestParam UUID productId){
        return ApiResponse.success(reservations.getAvailableInventory(productId));
    }
    @GetMapping("/lots")
    public ApiResponse<List<StockLot>> lots(@RequestParam UUID productId){
        products.getInventoryProduct(productId);
        return ApiResponse.success(lots.findByProductIdOrderByExpiryDateAscCreatedAtAsc(productId));
    }
    @GetMapping("/low-stock")
    public ApiResponse<List<ProductResponse>> lowStock(){
        return ApiResponse.success(products.listLowStockProducts());
    }
    @GetMapping("/expiring-lots")
    public ApiResponse<List<StockLot>> expiring(@RequestParam(defaultValue="30") @Min(0) @Max(3650) int withinDays){
        LocalDate today=LocalDate.now();return ApiResponse.success(lots.findExpiringLots(LotStatus.STORAGE,today,today.plusDays(withinDays)));
    }
}
