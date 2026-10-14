package com.manhnv.capstonebakerai.shipment.service;

import com.manhnv.capstonebakerai.common.exception.AppException;
import com.manhnv.capstonebakerai.shipment.dto.response.ShippingRateResponse;
import com.manhnv.capstonebakerai.shipment.exception.ShipmentErrorCode;
import com.manhnv.capstonebakerai.shipment.provider.goship.GoshipClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ShippingRateServiceImpl implements ShippingRateService {

    private final GoshipClient client;

    @Override
    public List<ShippingRateResponse> getRates(String provinceCode, String districtCode, String wardCode, int weightGram, BigDecimal cod, BigDecimal declaredValue) {
        return client.rates(provinceCode, districtCode, wardCode, weightGram, cod, declaredValue).stream()
                .map(r -> new ShippingRateResponse(r.id(), r.carrierName(), r.carrierLogo(), r.service(),
                        r.expected(), r.codFee(), r.totalFee(),
                        // Nếu total_amount của sandbox không bằng total_fee + cod_fee, đổi thành:
                        // r.totalFee().add(r.codFee() != null ? r.codFee() : BigDecimal.ZERO)
                        r.totalAmount()))
                .toList();
    }

    @Override
    public ShippingRateResponse requireRate(String provinceCode, String districtCode, String wardCode, int weightGram, BigDecimal cod, BigDecimal declaredValue, String rateId) {
        return getRates(provinceCode, districtCode, wardCode, weightGram, cod, declaredValue).stream()
                .filter(r -> r.id().equals(rateId))
                .findFirst()
                .orElseThrow(() -> new AppException(ShipmentErrorCode.SHIPPING_RATE_NOT_FOUND));
    }
}
