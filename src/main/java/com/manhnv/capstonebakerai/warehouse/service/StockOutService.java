package com.manhnv.capstonebakerai.warehouse.service;

import com.manhnv.capstonebakerai.warehouse.dto.StockOutCreateRequest;
import com.manhnv.capstonebakerai.warehouse.dto.StockOutDetailResponse;
import com.manhnv.capstonebakerai.warehouse.dto.StockLine;
import com.manhnv.capstonebakerai.warehouse.entity.StockOutNote;
import com.manhnv.capstonebakerai.warehouse.enums.NoteStatus;
import com.manhnv.capstonebakerai.warehouse.enums.StockOutType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.UUID;
import java.util.List;

public interface StockOutService {
    Page<StockOutNote> list(NoteStatus status, StockOutType type, Pageable pageable);
    StockOutDetailResponse get(UUID noteId);
    StockOutNote create(StockOutCreateRequest request);
    StockOutNote complete(UUID noteId);
    StockOutNote cancel(UUID noteId);
    StockOutNote processSaleOrder(UUID orderId, List<StockLine> orderItems);
    void cancelSaleOrder(UUID orderId);
}
