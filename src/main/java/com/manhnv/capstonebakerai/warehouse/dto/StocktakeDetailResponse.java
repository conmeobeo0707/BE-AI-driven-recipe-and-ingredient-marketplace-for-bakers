package com.manhnv.capstonebakerai.warehouse.dto;
import com.manhnv.capstonebakerai.warehouse.entity.StocktakeItem;
import com.manhnv.capstonebakerai.warehouse.entity.StocktakeNote;
import java.util.List;
import java.util.UUID;
public record StocktakeDetailResponse(StocktakeNote note, List<ProductLots> products) {
    public record ProductLots(UUID productId, List<StocktakeItem> lots) { }
}
