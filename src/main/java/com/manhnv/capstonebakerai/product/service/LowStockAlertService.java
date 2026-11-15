package com.manhnv.capstonebakerai.product.service;

import com.manhnv.capstonebakerai.product.dtos.LowStockAlertSummary;

public interface LowStockAlertService {
    LowStockAlertSummary sendLowStockAlerts();
}
