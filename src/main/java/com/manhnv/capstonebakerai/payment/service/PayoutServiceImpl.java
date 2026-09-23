package com.manhnv.capstonebakerai.payment.service;

import com.manhnv.capstonebakerai.common.exception.AppException;
import com.manhnv.capstonebakerai.payment.dto.request.PayoutRejectRequest;
import com.manhnv.capstonebakerai.payment.dto.request.PayoutRequest;
import com.manhnv.capstonebakerai.payment.dto.response.PayoutResponse;
import com.manhnv.capstonebakerai.payment.entity.Payout;
import com.manhnv.capstonebakerai.payment.entity.Wallet;
import com.manhnv.capstonebakerai.payment.entity.WalletTransaction;
import com.manhnv.capstonebakerai.payment.enums.PayoutStatus;
import com.manhnv.capstonebakerai.payment.enums.TransactionType;
import com.manhnv.capstonebakerai.payment.exception.PayoutError;
import com.manhnv.capstonebakerai.payment.exception.WalletErrorCode;
import com.manhnv.capstonebakerai.payment.gateway.PayoutGateway;
import com.manhnv.capstonebakerai.payment.mapper.PayoutMapper;
import com.manhnv.capstonebakerai.payment.repository.PayoutRepository;
import com.manhnv.capstonebakerai.payment.repository.WalletRepository;
import com.manhnv.capstonebakerai.payment.repository.WalletTransactionRepository;
import jakarta.persistence.*;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import vn.payos.PayOS;

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
    PayoutGateway payoutGateway;

    @Override
    @Transactional
    public PayoutResponse createPayoutRequest(UUID storeId, PayoutRequest request) {
        Wallet wallet = walletService.getWalletByStoreId(storeId);

        validatePayoutAmount(wallet,  request.getAmount());

        reserveBalance(wallet, request.getAmount());

        Payout payout = createPayout(storeId, request);
        payoutRepository.save(payout);

        log.info("Payout request created [{}] for store [{}], amount={}",
                payout.getPayoutId(), storeId, payout.getAmount());

        return payoutMapper.toPayoutResponse(payout);
    }

    @Override
    @Transactional
    public PayoutResponse approvePayout(UUID payoutId) {
        Payout payout = getPendingPayoutOrThrow(payoutId);
        Wallet wallet = walletService.getWalletByStoreId(payout.getStoreId());

        BigDecimal balanceBeforeSettle = wallet.getTotalBalance();

        // Bước 1: Trừ tiền trong locked
        settleLockedBalance(wallet, payout.getAmount());

        payout.setStatus(PayoutStatus.APPROVED);
        payout.setApprovedAt(LocalDateTime.now());
        payoutRepository.save(payout);

        log.info("Payout [{}] approved for store [{}], amount={}", payout.getPayoutId(), payout.getStoreId(), payout.getAmount());

        // Bước 2: Tự động thanh toán cho store
        try {
            processStoreBankPayout(payout);

            payout.setStatus(PayoutStatus.COMPLETED);
            payout.setCompletedAt(LocalDateTime.now());
            payoutRepository.save(payout);

            // Ghi nhận giao dịch trong sổ cái ví
            recordWalletTransaction(wallet, payout, balanceBeforeSettle);

            log.info("Payout [{}] completed, payment sent to store bank", payout.getPayoutId());
        } catch (Exception e) {
            // Thanh toán thất bại hoặc StoreBank chưa có -> đánh dấu FAILED và hoàn tiền lại ví
            log.error("Payout [{}] payment failed: {}", payout.getPayoutId(), e.getMessage());

            payout.setStatus(PayoutStatus.FAILED);
            payout.setFailedAt(LocalDateTime.now());
            payoutRepository.save(payout);

            refundFailedPayout(wallet, payout.getAmount());
        }

        return payoutMapper.toPayoutResponse(payout);
    }

    @Override
    @Transactional
    public PayoutResponse rejectPayout(UUID payoutId, PayoutRejectRequest request) {
        Payout payout = getPendingPayoutOrThrow(payoutId);

        Wallet wallet = walletService.getWalletByStoreId(payout.getStoreId());

        releaseLockedBalance(wallet, payout.getAmount());

        payout.setStatus(PayoutStatus.REJECTED);
        payout.setRejectReason(request.getReason());
        payout.setRejectedAt(LocalDateTime.now());
        payoutRepository.save(payout);

        log.info("Payout [{}] rejected for store [{}], amount={}, reason={}",
                payout.getPayoutId(), payout.getStoreId(), payout.getAmount(), request.getReason());

        return payoutMapper.toPayoutResponse(payout);
    }

    private void validatePayoutAmount(Wallet wallet, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new AppException(WalletErrorCode.INVALID_PAYOUT_AMOUNT);
        }

        if (wallet.getAvailableBalance().compareTo(amount) < 0) {
            throw new AppException(WalletErrorCode.INSUFFICIENT_BALANCE);
        }
    }

    private Payout getPendingPayoutOrThrow(UUID payoutId) {
        Payout payout = payoutRepository.findById(payoutId)
                .orElseThrow(() -> new AppException(PayoutError.PAYOUT_NOT_FOUND));

        if (payout.getStatus() != PayoutStatus.PENDING) {
            throw new AppException(PayoutError.PAYOUT_ALREADY_PROCESSED, "Payout is already in status: " + payout.getStatus());
        }

        return payout;
    }

    private Payout createPayout(UUID storeId, PayoutRequest request) {
        return Payout.builder()
                .storeId(storeId)
                .amount(request.getAmount())
                .status(PayoutStatus.PENDING)
                .build();
    }

    private void reserveBalance(Wallet wallet, BigDecimal amount) {
        wallet.setAvailableBalance(wallet.getAvailableBalance().subtract(amount));
        wallet.setLockedBalance(wallet.getLockedBalance().add(amount));
        walletRepository.save(wallet);
    }

    // Reject: Trả tiền về availableBalance, bỏ khỏi lockedBalance
    private void releaseLockedBalance(Wallet wallet, BigDecimal amount) {
        wallet.setLockedBalance(wallet.getLockedBalance().subtract(amount));
        wallet.setAvailableBalance(wallet.getAvailableBalance().add(amount));
        walletRepository.save(wallet);
    }

    // Approve: trừ locked + trừ total
    private void settleLockedBalance(Wallet wallet, BigDecimal amount) {
        wallet.setLockedBalance(wallet.getLockedBalance().subtract(amount));
        wallet.setTotalBalance(wallet.getTotalBalance().subtract(amount));
        walletRepository.save(wallet);
    }

    // Nếu thanh toán thất bại sau khi đã settle -> hoàn total + available lại
    private void refundFailedPayout(Wallet wallet, BigDecimal amount) {
        wallet.setTotalBalance(wallet.getTotalBalance().add(amount));
        wallet.setAvailableBalance(wallet.getAvailableBalance().add(amount));
        walletRepository.save(wallet);
    }

    /**
     * TODO: Khi có StoreBankRepository, thay đoạn hardcode bên dưới bằng:
     * StoreBank storeBank = storeBankRepository.findByStoreId(payout.getStoreId())
     *         .orElseThrow(() -> new AppException(PayoutError.STORE_BANK_NOT_FOUND));
     * rồi lấy accountNumber/accountName/bankCode từ storeBank thay vì hardcode.
     *
     * PayoutGateway.transfer() bên dưới chính là nơi PayOS (hoặc gateway khác)
     * sẽ được gọi thật sự — xem implementation trong package `gateway`.
     */
    private void processStoreBankPayout(Payout payout) {
        // Get StoreBank

        PayoutGateway.PayoutTransferRequest transferRequest = new PayoutGateway.PayoutTransferRequest(
                payout.getPayoutId(),
                "123",
                "NGUYEN VAN A",
                "CodeBank1234",
                payout.getAmount(),
                "Payout for store " + payout.getStoreId()
        );

        PayoutGateway.PayoutTransferResult result = payoutGateway.transfer(transferRequest);

        if (!result.success()) {
            throw new AppException(PayoutError.PAYOUT_PROCESSING_FAILED, "Bank transfer failed: " + result.failureReason());
        }

        payout.setBankTransactionId(result.externalTransactionId());
        // set processed (mai mốt bỏ id người xử lý vào)
        payout.setProcessedBy(UUID.randomUUID());
    }

    private void recordWalletTransaction(Wallet wallet, Payout payout, BigDecimal balanceBefore) {
        WalletTransaction walletTransaction = WalletTransaction.builder()
                .wallet(wallet)
                .amount(payout.getAmount().negate())
                .description("Payout via bank transfer, bankTransactionId=" + payout.getBankTransactionId())
                .transactionType(TransactionType.WITHDRAW)
                .referenceId(payout.getPayoutId())
                .balanceBefore(balanceBefore)
                .balanceAfter(wallet.getTotalBalance())
                .build();
        walletTransactionRepository.save(walletTransaction);
    }
}
