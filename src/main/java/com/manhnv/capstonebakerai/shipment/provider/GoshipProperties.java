package com.manhnv.capstonebakerai.shipment.provider;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "goship")
public record GoshipProperties(String baseUrl, String token, Pickup pickup, Parcel parcel) {
    public record Pickup(String name, String phone, String street, String city, String district, String ward) {}
    public record Parcel(int width, int height, int length) {}
}
