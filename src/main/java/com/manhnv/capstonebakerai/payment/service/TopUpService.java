package com.manhnv.capstonebakerai.payment.service;

import com.manhnv.capstonebakerai.payment.dto.request.TopUpRequest;
import com.manhnv.capstonebakerai.payment.dto.response.TopUpResponse;
import com.manhnv.capstonebakerai.payment.dto.response.TopUpTransactionResponse;
import com.manhnv.capstonebakerai.payment.entity.TopUpTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import vn.payos.model.webhooks.Webhook;

import java.util.UUID;

public interface TopUpService {

    TopUpResponse createTopUp(UUID storeId, TopUpRequest request);

    void handleWebhook(Webhook webhook);

    Page<TopUpTransactionResponse> getTopUpHistory(UUID storeId, Pageable pageable);

    TopUpTransactionResponse getTopUpById(UUID topUpId);
}
