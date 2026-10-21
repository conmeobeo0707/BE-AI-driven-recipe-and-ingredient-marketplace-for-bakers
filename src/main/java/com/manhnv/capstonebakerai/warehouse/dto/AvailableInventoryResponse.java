package com.manhnv.capstonebakerai.warehouse.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record AvailableInventoryResponse(UUID productId, BigDecimal saleableOnHandQuantity, BigDecimal reservedQuantity,
                                         BigDecimal availableQuantity) { }
