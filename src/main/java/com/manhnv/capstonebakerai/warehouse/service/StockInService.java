package com.manhnv.capstonebakerai.warehouse.service;

import com.manhnv.capstonebakerai.warehouse.dto.StockInCreateRequest;
import com.manhnv.capstonebakerai.warehouse.dto.StockInDetailResponse;
import com.manhnv.capstonebakerai.warehouse.entity.StockInNote;
import com.manhnv.capstonebakerai.warehouse.enums.NoteStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.UUID;

public interface StockInService {
    Page<StockInNote> list(NoteStatus status, Pageable pageable);
    StockInDetailResponse get(UUID noteId);
    StockInNote create(StockInCreateRequest request);
    StockInNote complete(UUID noteId);
    StockInNote cancel(UUID noteId);
}
