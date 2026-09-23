package com.manhnv.capstonebakerai.payment.service;

import com.manhnv.capstonebakerai.payment.dto.request.PayoutRejectRequest;
import com.manhnv.capstonebakerai.payment.dto.request.PayoutRequest;
import com.manhnv.capstonebakerai.payment.dto.response.PayoutResponse;

import java.util.UUID;

public interface PayoutService {

    PayoutResponse createPayoutRequest(UUID storeId, PayoutRequest request);

    PayoutResponse approvePayout(UUID payoutId);

    PayoutResponse rejectPayout(UUID payoutId, PayoutRejectRequest request);

}
