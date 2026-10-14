package com.manhnv.capstonebakerai.shipment.service;

import com.manhnv.capstonebakerai.shipment.dto.response.ShippingRateResponse;

import java.math.BigDecimal;
import java.util.List;

public interface ShippingRateService {
    List<ShippingRateResponse> getRates(String provinceCode, String districtCode, String wardCode, int weightGram, BigDecimal cod, BigDecimal declaredValue);

    ShippingRateResponse requireRate(String provinceCode, String districtCode, String wardCode, int weightGram, BigDecimal cod, BigDecimal declaredValue, String rateId);
}
