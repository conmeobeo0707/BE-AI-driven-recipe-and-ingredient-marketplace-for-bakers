package com.manhnv.capstonebakerai.quote.mapper;

import com.manhnv.capstonebakerai.product.entities.Product;
import com.manhnv.capstonebakerai.product.entities.ProductUnit;

import java.util.Map;
import java.util.UUID;

public record QuoteLookup(Map<UUID, ProductUnit> units, Map<UUID, Product> products) {
}
