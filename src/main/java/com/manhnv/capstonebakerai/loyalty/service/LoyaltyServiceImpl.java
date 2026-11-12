package com.manhnv.capstonebakerai.loyalty.service;

import com.manhnv.capstonebakerai.common.exception.AppException;
import com.manhnv.capstonebakerai.customer.entity.Customer;
import com.manhnv.capstonebakerai.loyalty.config.LoyaltyProperties;
import com.manhnv.capstonebakerai.loyalty.entity.AiUsage;
import com.manhnv.capstonebakerai.loyalty.entity.LoyaltyTransaction;
import com.manhnv.capstonebakerai.loyalty.enums.AiFeature;
import com.manhnv.capstonebakerai.loyalty.enums.AiUsageStatus;
import com.manhnv.capstonebakerai.loyalty.enums.LoyaltyTransactionType;
import com.manhnv.capstonebakerai.loyalty.exception.LoyaltyErrorCode;
import com.manhnv.capstonebakerai.loyalty.repository.AiUsageRepository;
import com.manhnv.capstonebakerai.loyalty.repository.LoyaltyTransactionRepository;
import com.manhnv.capstonebakerai.loyalty.repository.LoyaltyWalletRepository;
import com.manhnv.capstonebakerai.order.entity.Order;
import com.manhnv.capstonebakerai.order.enums.OrderStatus;
import com.manhnv.capstonebakerai.order.repository.OrderRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LoyaltyServiceImpl implements LoyaltyService {

    private final LoyaltyWalletRepository walletRepository;
    private final LoyaltyTransactionRepository transactionRepository;
    private final AiUsageRepository aiUsageRepository;
    private final OrderRepository orderRepository;
    private final LoyaltyProperties properties;
    private final EntityManager entityManager;

    @Override
    @Transactional(readOnly = true)
    public int getBalance(UUID customerId) {
        return walletRepository.findBalance(customerId).orElse(0);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LoyaltyTransaction> getTransactions(UUID customerId, Pageable pageable) {
        return transactionRepository.findByCustomer_IdOrderByCreatedAtDesc(customerId, pageable);
    }

    @Override
    public int priceOf(AiFeature feature) {
        return switch (feature) {
            case INGREDIENT_IMAGE -> properties.getIngredientImagePrice();
            case RECIPE_VIDEO -> properties.getRecipeVideoPrice();
        };
    }

    @Override
    @Transactional(readOnly = true)
    public int expectedOrderPoints(UUID customerId, BigDecimal revenue) {
        return orderPoints(revenue, isFirstOrder(customerId));
    }

    @Override
    @Transactional
    public boolean grantWelcomePoints(UUID customerId) {
        int points = properties.getWelcomePoints();
        if (points <= 0
                || transactionRepository.existsByCustomer_IdAndType(customerId, LoyaltyTransactionType.WELCOME)) {
            return false;
        }
        credit(customerId, LoyaltyTransactionType.WELCOME, points, null, null, "Điểm chào mừng");
        return true;
    }

    @Override
    @Transactional
    public void handleOrderDelivered(UUID orderId) {
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null || order.getStatus() != OrderStatus.DELIVERED) {
            return;
        }
        UUID customerId = order.getCustomer().getId();
        BigDecimal revenue = order.getRevenue() == null ? BigDecimal.ZERO : order.getRevenue();
        String code = order.getCode();
        Instant placedAt = order.getCreatedAt() == null
                ? Instant.now()
                : order.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant();

        if (!transactionRepository.existsByOrder_IdAndType(orderId, LoyaltyTransactionType.ORDER_EARN)) {
            boolean firstOrder = isFirstOrder(customerId);
            int points = orderPoints(revenue, firstOrder);
            if (points > 0) {
                String note = "Tích điểm đơn " + code + (firstOrder ? " (nhân đôi đơn đầu tiên)" : "");
                credit(customerId, LoyaltyTransactionType.ORDER_EARN, points, orderId, null, note);
            }
        }

        if (revenue.compareTo(properties.getPurchaseRefundMinRevenue()) < 0
                || transactionRepository.existsByOrder_IdAndType(orderId, LoyaltyTransactionType.AI_REFUND_PURCHASE)) {
            return;
        }
        Instant from = placedAt.minus(Duration.ofDays(properties.getPurchaseRefundDays()));
        Optional<AiUsage> latest = aiUsageRepository
                .findFirstByCustomer_IdAndStatusAndUsedAtBetweenOrderByPointsChargedDescUsedAtDesc(
                        customerId, AiUsageStatus.CHARGED, from, placedAt);
        if (latest.isEmpty()) {
            return;
        }
        UUID usageId = latest.get().getId();
        int charged = latest.get().getPointsCharged();
        Order orderRef = entityManager.getReference(Order.class, orderId);
        if (aiUsageRepository.markRefundedByPurchase(usageId, orderRef) == 1 && charged > 0) {
            credit(customerId, LoyaltyTransactionType.AI_REFUND_PURCHASE, charged, orderId, usageId,
                    "Hoàn điểm AI khi mua đơn " + code);
        }
    }

    @Override
    @Transactional
    public UUID chargeForAi(UUID customerId, AiFeature feature) {
        int price = Math.max(0, priceOf(feature));
        if (price > 0 && walletRepository.debit(customerId, price) == 0) {
            throw new AppException(
                    LoyaltyErrorCode.INSUFFICIENT_POINTS,
                    "You need " + price + " points to use this feature"
            );
        }

        AiUsage usage = new AiUsage();
        usage.setCustomer(entityManager.getReference(Customer.class, customerId));
        usage.setFeature(feature);
        usage.setPointsCharged(price);
        UUID usageId = aiUsageRepository.saveAndFlush(usage).getId();

        if (price > 0) {
            record(customerId, LoyaltyTransactionType.AI_SPEND, -price, null, usageId, spendNote(feature));
        }
        return usageId;
    }

    @Override
    @Transactional
    public boolean refundAiError(UUID aiUsageId, String reason) {
        AiUsage usage = aiUsageRepository.findById(aiUsageId)
                .orElseThrow(() -> new AppException(LoyaltyErrorCode.AI_USAGE_NOT_FOUND));
        UUID customerId = usage.getCustomer().getId();
        int charged = usage.getPointsCharged();

        if (aiUsageRepository.markRefundedByError(aiUsageId) == 0) {
            return false;
        }
        if (charged > 0) {
            credit(customerId, LoyaltyTransactionType.AI_REFUND_ERROR, charged, null, aiUsageId,
                    "Hoàn điểm: " + reason);
        }
        return true;
    }

    private boolean isFirstOrder(UUID customerId) {
        return !transactionRepository.existsByCustomer_IdAndType(customerId, LoyaltyTransactionType.ORDER_EARN);
    }

    private int orderPoints(BigDecimal revenue, boolean firstOrder) {
        if (revenue == null || revenue.signum() <= 0 || properties.getVndPerPoint().signum() <= 0) {
            return 0;
        }
        long points = revenue.divide(properties.getVndPerPoint(), 0, RoundingMode.DOWN).longValue();
        if (firstOrder) {
            points *= Math.max(1, properties.getFirstOrderMultiplier());
        }
        return (int) Math.min(points, properties.getMaxPointsPerOrder());
    }

    private void credit(UUID customerId, LoyaltyTransactionType type, int points,
                        UUID orderId, UUID aiUsageId, String note) {
        walletRepository.createIfMissing(customerId);
        walletRepository.credit(customerId, points);
        record(customerId, type, points, orderId, aiUsageId, note);
    }

    private void record(UUID customerId, LoyaltyTransactionType type, int points,
                        UUID orderId, UUID aiUsageId, String note) {
        LoyaltyTransaction transaction = new LoyaltyTransaction();
        transaction.setCustomer(entityManager.getReference(Customer.class, customerId));
        transaction.setType(type);
        transaction.setPoints(points);
        transaction.setBalanceAfter(walletRepository.findBalance(customerId).orElse(0));
        transaction.setOrder(orderId == null ? null : entityManager.getReference(Order.class, orderId));
        transaction.setAiUsage(aiUsageId == null ? null : entityManager.getReference(AiUsage.class, aiUsageId));
        transaction.setNote(note);
        transactionRepository.saveAndFlush(transaction);
    }

    private String spendNote(AiFeature feature) {
        return switch (feature) {
            case INGREDIENT_IMAGE -> "Nhận diện nguyên liệu bằng ảnh";
            case RECIPE_VIDEO -> "Tóm tắt công thức từ video";
        };
    }
}
