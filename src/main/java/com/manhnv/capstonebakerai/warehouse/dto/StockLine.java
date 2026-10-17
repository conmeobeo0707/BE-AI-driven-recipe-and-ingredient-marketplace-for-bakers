package com.manhnv.capstonebakerai.warehouse.dto;

import java.util.UUID;

public record StockLine(UUID productId, UUID productUnitId, int quantity) {}
