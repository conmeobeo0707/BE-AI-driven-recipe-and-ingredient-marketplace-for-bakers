package com.manhnv.capstonebakerai.shipment.enums;

import java.util.Optional;

public enum GoshipStatusCode {
    ;

    public static Optional<ShipmentStatus> toShipmentStatus(int code) {
        return switch (code) {
            case 900, 901 -> Optional.of(ShipmentStatus.PENDING);
            case 902 -> Optional.of(ShipmentStatus.PICKING);
            case 903, 904, 919 -> Optional.of(ShipmentStatus.IN_TRANSIT);
            case 905 -> Optional.of(ShipmentStatus.DELIVERED);
            case 906 -> Optional.of(ShipmentStatus.FAILED);
            case 907, 908 -> Optional.of(ShipmentStatus.RETURNED);
            case 914 -> Optional.of(ShipmentStatus.CANCELLED);
            default -> Optional.empty();
        };
    }
}
