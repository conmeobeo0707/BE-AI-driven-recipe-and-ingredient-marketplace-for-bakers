package com.manhnv.capstonebakerai.product.dtos;

import java.math.BigDecimal;
import java.util.UUID;

public record ComboItemRequest(UUID productId, BigDecimal quantity) { }
