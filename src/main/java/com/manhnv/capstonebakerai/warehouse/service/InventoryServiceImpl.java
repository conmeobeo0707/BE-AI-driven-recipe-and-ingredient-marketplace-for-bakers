package com.manhnv.capstonebakerai.warehouse.service;

import com.manhnv.capstonebakerai.warehouse.dto.StockLine;
import com.manhnv.capstonebakerai.warehouse.entity.StockLot;
import com.manhnv.capstonebakerai.warehouse.enums.LotStatus;
import com.manhnv.capstonebakerai.warehouse.repository.StockLotRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class InventoryServiceImpl implements InventoryService {
    private final StockOutService stockOutService;
    private final StockLotRepository stockLots;

    public InventoryServiceImpl(StockOutService stockOutService, StockLotRepository stockLots) {
        this.stockOutService = stockOutService;
        this.stockLots = stockLots;
    }

    @Override
    @Transactional(readOnly = true)
    public List<StockLot> getExpiringLots(int withinDays) {
        LocalDate today = LocalDate.now();
        return stockLots.findExpiringLots(LotStatus.STORAGE, today, today.plusDays(withinDays));
    }

    @Override
    @Transactional(readOnly = true)
    public List<StockLot> getExpiredLots() {
        return stockLots.findExpiredLots(List.of(LotStatus.STORAGE, LotStatus.EXPIRED), LocalDate.now());
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
