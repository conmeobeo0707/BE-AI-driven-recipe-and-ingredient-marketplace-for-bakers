package com.manhnv.capstonebakerai.payment.service;

import com.manhnv.capstonebakerai.common.exception.AppException;
import com.manhnv.capstonebakerai.payment.dto.request.TopUpRequest;
import com.manhnv.capstonebakerai.payment.dto.response.TopUpResponse;
import com.manhnv.capstonebakerai.payment.dto.response.TopUpTransactionResponse;
import com.manhnv.capstonebakerai.payment.entity.TopUpTransaction;
import com.manhnv.capstonebakerai.payment.entity.Wallet;
import com.manhnv.capstonebakerai.payment.entity.WalletTransaction;
import com.manhnv.capstonebakerai.payment.enums.PaymentMethod;
import com.manhnv.capstonebakerai.payment.enums.TopUpStatus;
import com.manhnv.capstonebakerai.payment.enums.TransactionType;
import com.manhnv.capstonebakerai.payment.exception.WalletErrorCode;
import com.manhnv.capstonebakerai.payment.mapper.TopUpTransactionMapper;
import com.manhnv.capstonebakerai.payment.repository.TopUpTransactionRepository;
import com.manhnv.capstonebakerai.payment.repository.WalletRepository;
import com.manhnv.capstonebakerai.payment.repository.WalletTransactionRepository;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import vn.payos.PayOS;
import vn.payos.exception.PayOSException;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkRequest;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkResponse;
import vn.payos.model.webhooks.Webhook;
import vn.payos.model.webhooks.WebhookData;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TopUpServiceImpl implements TopUpService {

    TopUpTransactionRepository topUpTransactionRepository;
    WalletRepository walletRepository;
    WalletTransactionRepository walletTransactionRepository;
    WalletService walletService;
    TopUpTransactionMapper topUpTransactionMapper;
    PayOS payOS;

    @NonFinal
    @Value("${payos.return-url}")
    private String returnUrl;

    @NonFinal
    @Value("${payos.cancel-url}")
    private String cancelUrl;

    private static final String PAYOS_SUCCESS_CODE = "00";

    @Override
    @Transactional
    public TopUpResponse createTopUp(UUID storeId, TopUpRequest request) {
        walletService.getWalletByStoreId(storeId);

        long orderCode = System.currentTimeMillis() / 1000;

        TopUpTransaction transaction = createTransaction(storeId, request, orderCode);
        topUpTransactionRepository.save(transaction);

        CreatePaymentLinkRequest paymentData = createPaymentLinkRequest(orderCode, request, returnUrl,cancelUrl);

        CreatePaymentLinkResponse response = createPaymentLinkResponse(paymentData, orderCode, transaction);

        return TopUpResponse.builder()
                .topUpId(transaction.getTopupId())
                .storeId(storeId)
                .amount(transaction.getAmount())
                .status(transaction.getStatus())
                .checkoutUrl(response.getCheckoutUrl())
                .createdAt(transaction.getCreatedAt())
                .build();
    }

    private TopUpTransaction createTransaction(UUID storeId, TopUpRequest request, long orderCode) {
        return TopUpTransaction.builder()
                .storeId(storeId)
                .amount(request.getAmount())
                .paymentMethod(PaymentMethod.BANK_TRANSFER)
                .externalTransactionId(String.valueOf(orderCode))
                .status(TopUpStatus.PENDING)
                .build();
    }

    private CreatePaymentLinkRequest createPaymentLinkRequest(long orderCode, TopUpRequest request, String returnUrl, String cancelUrl) {

        return CreatePaymentLinkRequest.builder()
                .orderCode(orderCode)
                .amount(request.getAmount().longValue())
                .description("Nạp tiền vào ví Store")
                .returnUrl(returnUrl)
                .cancelUrl(cancelUrl)
                .build();
    }

    private CreatePaymentLinkResponse createPaymentLinkResponse(CreatePaymentLinkRequest paymentData, long orderCode, TopUpTransaction transaction) {
        CreatePaymentLinkResponse response;
        try {
            response = payOS.paymentRequests().create(paymentData);
        } catch (PayOSException e) {
            log.error("PayOS create payment link failed for orderCode={}", orderCode, e);
            transaction.setStatus(TopUpStatus.FAILED);
            topUpTransactionRepository.save(transaction);
            throw new AppException(WalletErrorCode.INVALID_TOPUP_AMOUNT, "Failed to create PayOS payment link: " + e.getMessage());
        }

        return response;
    }


    @Override
    @Transactional
    public void handleWebhook(Webhook webhook) {
        WebhookData data;
        try {
            data = payOS.webhooks().verify(webhook);
        } catch (Exception e) {
            log.warn("Invalid PayOS webhook signature: {}", e.getMessage());
            return;
        }

        String orderCode = String.valueOf(data.getOrderCode());

        TopUpTransaction transaction = topUpTransactionRepository
                .findByExternalTransactionId(orderCode)
                .orElse(null);

        if (transaction == null) {
            log.warn("Webhook received for unknow orderCode={}", orderCode);
            return;
        }

        if (transaction.getStatus() != TopUpStatus.PENDING) {
            log.info("TopUp [{}] already processed with status={}, skip", transaction.getTopupId(), transaction.getStatus());
            return;
        }

        if (!PAYOS_SUCCESS_CODE.equals(data.getCode())) {
            transaction.setStatus(TopUpStatus.FAILED);
            topUpTransactionRepository.save(transaction);
            log.info("TopUp [{}] failed, PayOS code={}", transaction.getTopupId(), data.getCode());
            return;
        }

        transaction.setStatus(TopUpStatus.SUCCESS);
        topUpTransactionRepository.save(transaction);

        Wallet wallet = walletRepository.findByStoreId(transaction.getStoreId())
                .orElseThrow(() -> new AppException(WalletErrorCode.WALLET_NOT_FOUND, "Wallet not found for store: " + transaction.getStoreId()));

        BigDecimal balanceBefore = wallet.getTotalBalance();
        BigDecimal amount = transaction.getAmount();

        wallet.setTotalBalance(wallet.getTotalBalance().add(amount));
        wallet.setAvailableBalance(wallet.getAvailableBalance().add(amount));
        walletRepository.save(wallet);

        WalletTransaction walletTransaction = WalletTransaction.builder()
                .wallet(wallet)
                .amount(amount)
                .description("Top-up via PayOS, orderCode=" + orderCode)
                .transactionType(TransactionType.DEPOSIT)
                .referenceId(transaction.getTopupId())
                .balanceBefore(balanceBefore)
                .balanceAfter(wallet.getTotalBalance())
                .build();
        walletTransactionRepository.save(walletTransaction);

        log.info("TopUp [{}] success, wallet [{}] balance updated: {} -> {}", transaction.getTopupId(), wallet.getStoreWalletId(), balanceBefore, wallet.getTotalBalance());
    }

    @Override
    public Page<TopUpTransactionResponse> getTopUpHistory(UUID storeId, Pageable pageable) {
        return topUpTransactionRepository.findByStoreId(storeId, pageable)
                .map(this::toResponse);
    }

    @Override
    public TopUpTransactionResponse getTopUpById(UUID topUpId) {
        TopUpTransaction transaction = topUpTransactionRepository.findById(topUpId)
                .orElseThrow(() -> new AppException(WalletErrorCode.WALLET_NOT_FOUND));
        return toResponse(transaction);
    }

    private TopUpTransactionResponse toResponse(TopUpTransaction t) {
        return topUpTransactionMapper.toResponse(t);
    }
}
