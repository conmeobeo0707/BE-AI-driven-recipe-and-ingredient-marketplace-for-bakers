package com.manhnv.capstonebakerai.warehouse.dto;
import com.manhnv.capstonebakerai.warehouse.entity.StockInItem;
import com.manhnv.capstonebakerai.warehouse.entity.StockInNote;
import java.util.List;
public record StockInDetailResponse(StockInNote note, List<StockInItem> items) { }
