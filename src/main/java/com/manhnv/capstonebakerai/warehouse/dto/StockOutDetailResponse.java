package com.manhnv.capstonebakerai.warehouse.dto;
import com.manhnv.capstonebakerai.warehouse.entity.StockOutItem;
import com.manhnv.capstonebakerai.warehouse.entity.StockOutNote;
import java.util.List;
public record StockOutDetailResponse(StockOutNote note, List<StockOutItem> items) { }
