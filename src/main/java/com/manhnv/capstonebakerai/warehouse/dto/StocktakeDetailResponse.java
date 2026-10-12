package com.manhnv.capstonebakerai.warehouse.dto;
import com.manhnv.capstonebakerai.warehouse.entity.StocktakeItem;
import com.manhnv.capstonebakerai.warehouse.entity.StocktakeNote;
import java.util.List;
public record StocktakeDetailResponse(StocktakeNote note, List<StocktakeItem> items) { }
