package com.manhnv.capstonebakerai.shipment.provider.goship;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GoshipListResponse<T>(int code, String status, List<T> data) {
}
