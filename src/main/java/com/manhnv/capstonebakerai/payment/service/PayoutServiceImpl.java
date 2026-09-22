package com.manhnv.capstonebakerai.payment.service;

import com.manhnv.capstonebakerai.common.exception.AppException;
import com.manhnv.capstonebakerai.payment.dto.request.PayoutRequest;
import com.manhnv.capstonebakerai.payment.dto.request.TopUpRequest;
import com.manhnv.capstonebakerai.payment.dto.response.PayoutResponse;
import com.manhnv.capstonebakerai.payment.dto.response.TopUpResponse;
import com.manhnv.capstonebakerai.payment.entity.Payout;
import com.manhnv.capstonebakerai.payment.entity.TopUpTransaction;
import com.manhnv.capstonebakerai.payment.entity.Wallet;
import com.manhnv.capstonebakerai.payment.entity.WalletTransaction;
import com.manhnv.capstonebakerai.payment.enums.PayoutStatus;
import com.manhnv.capstonebakerai.payment.enums.TopUpStatus;
import com.manhnv.capstonebakerai.payment.enums.TransactionType;
import com.manhnv.capstonebakerai.payment.exception.WalletErrorCode;
import com.manhnv.capstonebakerai.payment.mapper.PayoutMapper;
import com.manhnv.capstonebakerai.payment.mapper.TopUpTransactionMapper;
import com.manhnv.capstonebakerai.payment.repository.PayoutRepository;
import com.manhnv.capstonebakerai.payment.repository.TopUpTransactionRepository;
import com.manhnv.capstonebakerai.payment.repository.WalletRepository;
import com.manhnv.capstonebakerai.payment.repository.WalletTransactionRepository;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import vn.payos.PayOS;
import vn.payos.exception.PayOSException;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkRequest;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkResponse;
import vn.payos.model.webhooks.Webhook;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PayoutServiceImpl implements PayoutService {

    WalletRepository walletRepository;
    WalletTransactionRepository walletTransactionRepository;
    WalletService walletService;
    PayoutMapper payoutMapper;
    PayoutRepository payoutRepository;
    PayOS payOS;


    @NonFinal
    @Value("${payos.return-url}")
    private String returnUrl;

    @NonFinal
    @Value("${payos.cancel-url}")
    private String cancelUrl;

    private static final String PAYOS_SUCCESS_CODE = "00";

    @Override
    public PayoutResponse createPayoutRequest(UUID storeId, PayoutRequest request) {
        Wallet wallet = walletService.getWalletByStoreId(storeId);
        // Kiểm tra ví còn đủ tiền đủ rút không

        long orderCode = System.currentTimeMillis() / 1000;

        Payout payout = Payout.builder()
                .storeId(storeId)
                .amount(request.getAmount())
                .status(PayoutStatus.PENDING)
                .build();
        payoutRepository.save(payout);

        CreatePaymentLinkRequest paymentData = createPaymentLinkRequest(orderCode, request, returnUrl,cancelUrl);

        CreatePaymentLinkResponse response = createPaymentLinkResponse(paymentData, orderCode, payout);

        return PayoutResponse.builder()
                .payoutId(payout.getPayoutId())
                .storeId(storeId)
                .amount(payout.getAmount())
                .status(payout.getStatus())
                .checkoutUrl(response.getCheckoutUrl())
                .createdAt(payout.getCreatedAt())
                .build();
    }

    private CreatePaymentLinkRequest createPaymentLinkRequest(long orderCode, PayoutRequest request, String returnUrl, String cancelUrl) {

        return CreatePaymentLinkRequest.builder()
                .orderCode(orderCode)
                .amount(request.getAmount().longValue())
                .description("Yêu cầu rút tiền của Store")
                .returnUrl(returnUrl)
                .cancelUrl(cancelUrl)
                .build();
    }

    private CreatePaymentLinkResponse createPaymentLinkResponse(CreatePaymentLinkRequest paymentData, long orderCode, Payout payout) {
        CreatePaymentLinkResponse response;
        try {
            response = payOS.paymentRequests().create(paymentData);
        } catch (PayOSException e) {
             log.error("PayOS create payment link failed for orderCode={}", orderCode, e);
            payout.setStatus(PayoutStatus.FAILED);
            payoutRepository.save(payout);
            throw new AppException(WalletErrorCode.INVALID_TOPUP_AMOUNT, "Failed to create PayOS payment link: " + e.getMessage());
        }

        return response;
    }

    @Override
    public void handleWebhook(Webhook webhook) {

    }


}
