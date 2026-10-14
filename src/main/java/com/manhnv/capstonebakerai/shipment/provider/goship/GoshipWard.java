package com.manhnv.capstonebakerai.shipment.provider.goship;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GoshipWard(String id, String name, @JsonProperty("district_id") String districtId) {
}
