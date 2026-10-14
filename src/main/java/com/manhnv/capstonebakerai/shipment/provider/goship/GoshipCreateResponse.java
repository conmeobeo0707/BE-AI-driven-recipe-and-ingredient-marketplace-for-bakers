package com.manhnv.capstonebakerai.shipment.provider.goship;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GoshipCreateResponse(
        int code,
        String status,
        String id,
        @JsonProperty("tracking_number")  String trackingNumber,
        @JsonProperty("carrier_short_name") String carrierShortName,
        @JsonProperty("carrier")          String carrier,
        @JsonProperty("cod")              String cod,
        @JsonProperty("fee")              String fee,
        @JsonProperty("created_at")       String createdAt
) {
}
