package com.manhnv.capstonebakerai.shipment.provider.goship;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GoshipDistrict(
        String id,
        String name,
        @JsonProperty("city_id") String cityId) {
}
