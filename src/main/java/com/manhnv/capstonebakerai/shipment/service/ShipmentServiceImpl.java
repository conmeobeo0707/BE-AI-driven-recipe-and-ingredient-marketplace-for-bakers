package com.manhnv.capstonebakerai.shipment.service;

import com.manhnv.capstonebakerai.common.exception.AppException;
import com.manhnv.capstonebakerai.order.entity.Order;
import com.manhnv.capstonebakerai.order.entity.OrderItem;
import com.manhnv.capstonebakerai.order.enums.PaymentMethod;
import com.manhnv.capstonebakerai.shipment.dto.response.ShipmentResponse;
import com.manhnv.capstonebakerai.shipment.entity.Shipment;
import com.manhnv.capstonebakerai.shipment.enums.ShipmentStatus;
import com.manhnv.capstonebakerai.shipment.exception.ShipmentErrorCode;
import com.manhnv.capstonebakerai.shipment.mapper.ShipmentMapper;
import com.manhnv.capstonebakerai.shipment.provider.ShippingProvider;
import com.manhnv.capstonebakerai.shipment.repository.ShipmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ShipmentServiceImpl implements ShipmentService {

    private final ShipmentRepository shipmentRepository;
    private final ShippingProvider provider;
    private final ShipmentMapper mapper;

    @Override
    @Transactional
    public ShipmentResponse createForOrder(Order order) {
        return shipmentRepository.findByOrderId(order.getId())
                .map(mapper::toResponse)
                .orElseGet(() -> {
                    BigDecimal codAmount = order.getPaymentMethod() == PaymentMethod.COD
                            ? order.getTotal() : BigDecimal.ZERO;
                    int weightGram = order.getItems().stream()
                            .mapToInt(i -> i.getWeightGram() * i.getQuantity())
                            .sum();
                    weightGram = Math.max(1, weightGram);

                    // Lưu ý: gọi API ngoài trong transaction. Với Goship thật nên xử lý lỗi/timeout cẩn thận.
                    var result = provider.createShipment(order, codAmount, weightGram);

                    Shipment shipment = Shipment.builder()
                            .order(order)
                            .provider(provider.name())
                            .carrier(result.carrier())
                            .trackingCode(result.trackingCode())
                            .externalId(result.externalId())
                            .shippingFee(order.getShippingFee())
                            .codAmount(codAmount)
                            .weightGram(weightGram)
                            .build();
                    return mapper.toResponse(shipmentRepository.save(shipment));
                });
    }

    @Override
    @Transactional
    public void cancelForOrder(UUID orderId) {
        shipmentRepository.findByOrderId(orderId).ifPresent(s -> {
            if (s.getStatus() == ShipmentStatus.PENDING || s.getStatus() == ShipmentStatus.PICKING) {
                provider.cancelShipment(s.getExternalId());
                s.setStatus(ShipmentStatus.CANCELLED);
                shipmentRepository.save(s);
            }
        });
    }

    @Override
    @Transactional(readOnly = true)
    public ShipmentResponse getByOrderId(UUID orderId) {
        return shipmentRepository.findByOrderId(orderId)
                .map(mapper::toResponse)
                .orElseThrow(() -> new AppException(ShipmentErrorCode.SHIPMENT_NOT_FOUND));
    }
}
