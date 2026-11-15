package com.manhnv.capstonebakerai.warehouse.controller;

import com.manhnv.capstonebakerai.common.response.ApiResponse;
import com.manhnv.capstonebakerai.warehouse.dto.StockTransactionFilter;
import com.manhnv.capstonebakerai.warehouse.dto.StockTransactionResponse;
import com.manhnv.capstonebakerai.warehouse.enums.StockTransactionRefType;
import com.manhnv.capstonebakerai.warehouse.enums.StockTransactionType;
import com.manhnv.capstonebakerai.warehouse.service.StockTransactionService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/warehouse/stock-transactions")
public class StockTransactionController {
    private final StockTransactionService service;

    public StockTransactionController(StockTransactionService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'STAFF')")
    public ApiResponse<Page<StockTransactionResponse>> list(
            @RequestParam(required = false) UUID productId,
            @RequestParam(required = false) UUID stockLotId,
            @RequestParam(required = false) StockTransactionType transactionType,
            @RequestParam(required = false) StockTransactionRefType refType,
            @RequestParam(required = false) UUID refId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        StockTransactionFilter filter = new StockTransactionFilter(
                productId, stockLotId, transactionType, refType, refId, from, to);
        return ApiResponse.success(service.search(filter, pageable));
    }
}
