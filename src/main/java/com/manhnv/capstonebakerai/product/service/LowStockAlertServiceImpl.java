package com.manhnv.capstonebakerai.product.service;

import com.manhnv.capstonebakerai.notification.enums.NotificationRefType;
import com.manhnv.capstonebakerai.notification.service.NotificationService;
import com.manhnv.capstonebakerai.product.dtos.LowStockAlertSummary;
import com.manhnv.capstonebakerai.product.dtos.ProductResponse;
import com.manhnv.capstonebakerai.user.entity.User;
import com.manhnv.capstonebakerai.user.enums.Role;
import com.manhnv.capstonebakerai.user.enums.UserStatus;
import com.manhnv.capstonebakerai.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LowStockAlertServiceImpl implements LowStockAlertService {
    private static final int ALERT_COOLDOWN_HOURS = 24;

    private final ProductService productService;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public LowStockAlertSummary sendLowStockAlerts() {
        List<ProductResponse> lowStockProducts = productService.listLowStockProducts();
        List<UUID> recipientIds = userRepository
                .findByRoleInAndStatusOrderByCreatedAtDescUserIdAsc(
                        List.of(Role.STAFF, Role.MANAGER), UserStatus.ACTIVE)
                .stream()
                .map(User::getUserId)
                .toList();

        if (lowStockProducts.isEmpty() || recipientIds.isEmpty()) {
            return new LowStockAlertSummary(lowStockProducts.size(), 0);
        }

        LocalDateTime cooldownStart = LocalDateTime.now().minusHours(ALERT_COOLDOWN_HOURS);
        int sentCount = 0;
        for (ProductResponse product : lowStockProducts) {
            for (UUID recipientId : recipientIds) {
                if (notificationService.existsForUserAfter(
                        recipientId, NotificationRefType.PRODUCT, product.productId(), cooldownStart)) {
                    continue;
                }
                notificationService.create(recipientId, "Low stock alert", alertContent(product),
                        NotificationRefType.PRODUCT, product.productId());
                sentCount++;
            }
        }
        return new LowStockAlertSummary(lowStockProducts.size(), sentCount);
    }

    private String alertContent(ProductResponse product) {
        String content = "Product " + product.name() + " (" + product.sku() + ") has "
                + product.stockQuantity() + " in stock; minimum is " + product.minStock() + ".";
        return content.length() <= 255 ? content : content.substring(0, 252) + "...";
    }
}
