package com.manhnv.capstonebakerai.shipment.exception;

import com.manhnv.capstonebakerai.common.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ShipmentErrorCode implements ErrorCode {
    SHIPMENT_NOT_FOUND("Shipment not found", HttpStatus.NOT_FOUND),
    WEBHOOK_UNAUTHORIZED("Invalid webhook token", HttpStatus.UNAUTHORIZED),
    GOSHIP_UNAVAILABLE("Shipping provider is unavailable", HttpStatus.BAD_GATEWAY),
    GOSHIP_CREATE_FAILED("Cannot create shipment at shipping provider", HttpStatus.BAD_GATEWAY),
    SHIPPING_RATE_NOT_FOUND("Selected shipping rate is no longer available, please choose again", HttpStatus.BAD_REQUEST),
    ;

    private final String message;
    private final HttpStatus httpStatus;
}
