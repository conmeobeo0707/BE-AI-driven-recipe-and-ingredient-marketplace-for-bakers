package com.manhnv.capstonebakerai.shipment.provider.goship;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GoshipCity(String id, String name) {
}
