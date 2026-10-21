package com.manhnv.capstonebakerai.quote.service;

import com.manhnv.capstonebakerai.quote.dto.request.MarkOrderedRequest;
import com.manhnv.capstonebakerai.quote.dto.request.RejectRequestQuoteRequest;
import com.manhnv.capstonebakerai.quote.dto.request.RequestQuoteRequest;
import com.manhnv.capstonebakerai.quote.dto.response.RequestQuoteResponse;
import com.manhnv.capstonebakerai.quote.dto.response.RequestQuoteSummaryResponse;
import com.manhnv.capstonebakerai.quote.enums.RequestQuoteStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface RequestQuoteService {
    // customer
    RequestQuoteResponse create(UUID userId, RequestQuoteRequest request);
    Page<RequestQuoteSummaryResponse> getMine(UUID userId, RequestQuoteStatus status, Pageable pageable);
    RequestQuoteResponse getMine(UUID userId, UUID id);
    RequestQuoteResponse updateMine(UUID userId, UUID id, RequestQuoteRequest request);
    RequestQuoteResponse cancelMine(UUID userId, UUID id);

    // staff / manager
    Page<RequestQuoteSummaryResponse> getAll(RequestQuoteStatus status, Pageable pageable);
    RequestQuoteResponse getForStaff(UUID id);
    RequestQuoteResponse claim(UUID id, UUID staffUserId);
    RequestQuoteResponse markOrdered(UUID id, UUID staffUserId, boolean manager, MarkOrderedRequest request);
    RequestQuoteResponse reject(UUID id, UUID staffUserId, boolean manager, RejectRequestQuoteRequest request);
}
