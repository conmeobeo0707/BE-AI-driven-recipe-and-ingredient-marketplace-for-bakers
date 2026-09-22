package com.manhnv.capstonebakerai.payment.service;

import com.manhnv.capstonebakerai.payment.dto.request.PayoutRequest;
import com.manhnv.capstonebakerai.payment.dto.request.TopUpRequest;
import com.manhnv.capstonebakerai.payment.dto.response.PayoutResponse;
import com.manhnv.capstonebakerai.payment.dto.response.TopUpResponse;
import vn.payos.model.webhooks.Webhook;

import java.util.UUID;

public interface PayoutService {

    PayoutResponse createPayoutRequest(UUID storeId, PayoutRequest request);

    void handleWebhook(Webhook webhook);
}
