package com.manhnv.capstonebakerai.warehouse.service;

import com.manhnv.capstonebakerai.warehouse.dto.StocktakeCountRequest;
import com.manhnv.capstonebakerai.warehouse.dto.StocktakeCreateRequest;
import com.manhnv.capstonebakerai.warehouse.dto.StocktakeDetailResponse;
import com.manhnv.capstonebakerai.warehouse.entity.StocktakeNote;
import com.manhnv.capstonebakerai.warehouse.enums.StocktakeStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.UUID;

public interface StocktakeService {
    Page<StocktakeNote> list(StocktakeStatus status, Pageable pageable);
    StocktakeDetailResponse get(UUID noteId);
    StocktakeDetailResponse create(StocktakeCreateRequest request);
    StocktakeDetailResponse count(UUID noteId, StocktakeCountRequest request);
    StocktakeDetailResponse complete(UUID noteId);
    StocktakeNote cancel(UUID noteId);
}
