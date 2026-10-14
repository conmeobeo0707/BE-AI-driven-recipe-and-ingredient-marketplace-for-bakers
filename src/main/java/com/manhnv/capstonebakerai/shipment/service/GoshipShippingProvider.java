package com.manhnv.capstonebakerai.shipment.service;

import com.manhnv.capstonebakerai.common.exception.AppException;
import com.manhnv.capstonebakerai.order.entity.Order;
import com.manhnv.capstonebakerai.shipment.provider.ShippingProvider;
import com.manhnv.capstonebakerai.shipment.provider.goship.GoshipClient;
import com.manhnv.capstonebakerai.shipment.provider.goship.GoshipCreateResponse;
import com.manhnv.capstonebakerai.shipment.provider.goship.GoshipRate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "shipping.provider", havingValue = "goship")
public class GoshipShippingProvider implements ShippingProvider {

    private final GoshipClient client;

    @Override
    public String name() {
        return "GOSHIP";
    }

    @Override
    public ShipmentCreateResult createShipment(Order order, BigDecimal codAmount, int weightGram) {
        GoshipCreateResponse res;
        try {
            res = send(order, order.getShippingRateId(), codAmount, weightGram);
        } catch (AppException e) {
            // Rate id có thể hết hạn: gọi lại rates, chọn đúng hãng + dịch vụ đã lưu, thử lại 1 lần
            log.warn("Create shipment with stored rate failed, retrying with fresh rates", e);
            GoshipRate fresh = client.rates(order.getProvinceCode(), order.getDistrictCode(), order.getWardCode(),
                            weightGram, codAmount, order.getProductsPrice()).stream()
                    .filter(r -> r.carrierName().equals(order.getCarrierName())
                            && r.service().equals(order.getShippingService()))
                    .findFirst()
                    .orElseThrow(() -> e);
            res = send(order, fresh.id(), codAmount, weightGram);
        }
        String carrier = res.carrierShortName() != null ? res.carrierShortName() : res.carrier();
        return new ShipmentCreateResult(res.id(), res.trackingNumber(), carrier);
    }

    private GoshipCreateResponse send(Order order, String rateId, BigDecimal cod, int weightGram) {
        return client.createShipment(rateId, order.getReceiverName(), order.getReceiverPhone(),
                order.getAddressDetail(), order.getProvinceCode(), order.getDistrictCode(), order.getWardCode(),
                weightGram, cod, order.getProductsPrice(), order.getNote());
    }

    @Override
    public void cancelShipment(String externalId) {
        // Sandbox không có API hủy. TODO production: gọi API hủy vận đơn của Goship.
        log.warn("Goship sandbox has no cancel API, shipment {} only marked CANCELLED locally", externalId);
    }
}
