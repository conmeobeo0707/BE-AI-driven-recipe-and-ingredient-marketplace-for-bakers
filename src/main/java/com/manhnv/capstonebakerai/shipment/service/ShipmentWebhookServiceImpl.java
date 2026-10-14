package com.manhnv.capstonebakerai.shipment.service;

import com.manhnv.capstonebakerai.common.exception.AppException;
import com.manhnv.capstonebakerai.order.enums.OrderStatus;
import com.manhnv.capstonebakerai.order.service.OrderService;
import com.manhnv.capstonebakerai.shipment.dto.request.ShipmentWebhookRequest;
import com.manhnv.capstonebakerai.shipment.entity.Shipment;
import com.manhnv.capstonebakerai.shipment.enums.GoshipStatusCode;
import com.manhnv.capstonebakerai.shipment.enums.ShipmentStatus;
import com.manhnv.capstonebakerai.shipment.exception.ShipmentErrorCode;
import com.manhnv.capstonebakerai.shipment.repository.ShipmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ShipmentWebhookServiceImpl implements ShipmentWebhookService{

    private final ShipmentRepository shipmentRepository;
    private final OrderService orderService;

    @Value("${shipping.webhook-secret}")
    private String webhookSecret;

    @Override
    @Transactional
    public void handle(String token, ShipmentWebhookRequest request) {
        if (token == null || !MessageDigest.isEqual(
                token.getBytes(StandardCharsets.UTF_8), webhookSecret.getBytes(StandardCharsets.UTF_8))) {
            throw new AppException(ShipmentErrorCode.WEBHOOK_UNAUTHORIZED);
        }

        Shipment shipment = shipmentRepository.findByTrackingCode(request.trackingCode())
                .orElseThrow(() -> new AppException(ShipmentErrorCode.SHIPMENT_NOT_FOUND));

        ShipmentStatus next = GoshipStatusCode.toShipmentStatus(request.statusCode())
                .orElse(null);
        if (next == null || shipment.getStatus() == next) {
            return;
        }

        shipment.setStatus(next);
        if (next == ShipmentStatus.IN_TRANSIT) shipment.setShippedAt(LocalDateTime.now());
        if (next == ShipmentStatus.DELIVERED) shipment.setDeliveredAt(LocalDateTime.now());
        shipmentRepository.save(shipment);

        OrderStatus target = switch (next) {
            case PICKING -> OrderStatus.READY_TO_SHIP;
            case IN_TRANSIT -> OrderStatus.SHIPPING;
            case DELIVERED -> OrderStatus.DELIVERED;
            default -> null; // PENDING, FAILED, RETURNED, CANCELLED
            // TODO: FAILED/RETURNED cần quy trình giao lại / hoàn hàng / hoàn kho
        };
        if (target != null) {
            orderService.syncStatusFromShipment(shipment.getOrder().getId(), target);
        }
    }
}
