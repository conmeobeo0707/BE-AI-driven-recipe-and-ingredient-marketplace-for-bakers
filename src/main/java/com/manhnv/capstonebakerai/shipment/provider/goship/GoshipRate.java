package com.manhnv.capstonebakerai.shipment.provider.goship;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GoshipRate(
        String id,
        @JsonProperty("carrier_name") String carrierName,
        @JsonProperty("carrier_logo") String carrierLogo,
        String service,
        String expected,
        @JsonProperty("cod_fee")BigDecimal codFee,
        @JsonProperty("total_fee") BigDecimal totalFee,
        @JsonProperty("total_amount") BigDecimal totalAmount
        ) {
}
