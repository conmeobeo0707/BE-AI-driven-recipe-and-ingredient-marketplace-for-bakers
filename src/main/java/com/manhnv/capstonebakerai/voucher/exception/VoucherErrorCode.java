package com.manhnv.capstonebakerai.voucher.exception;

import com.manhnv.capstonebakerai.common.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@AllArgsConstructor
@Getter
public enum VoucherErrorCode implements ErrorCode {
    VOUCHER_NOT_FOUND("Voucher not found", HttpStatus.NOT_FOUND),
    VOUCHER_CODE_EXISTED("Voucher code already exists", HttpStatus.CONFLICT),
    VOUCHER_INVALID_DATE_RANGE("End date must be after start date", HttpStatus.BAD_REQUEST),
    VOUCHER_INVALID_DISCOUNT_VALUE("Invalid discount value", HttpStatus.BAD_REQUEST),
    VOUCHER_NOT_ACTIVE("Voucher is not active", HttpStatus.BAD_REQUEST),
    VOUCHER_NOT_STARTED("Voucher has not started yet", HttpStatus.BAD_REQUEST),
    VOUCHER_EXPIRED("Voucher has expired", HttpStatus.BAD_REQUEST),
    VOUCHER_OUT_OF_STOCK("Voucher has run out", HttpStatus.BAD_REQUEST),
    VOUCHER_MIN_ORDER_NOT_MET("Order value does not meet voucher minimum", HttpStatus.BAD_REQUEST),
    VOUCHER_USAGE_LIMIT_REACHED("You have reached the usage limit for this voucher", HttpStatus.BAD_REQUEST),
    VOUCHER_ALREADY_USED_BY_USAGE("Voucher already has usages, this field cannot be changed", HttpStatus.BAD_REQUEST),
    VOUCHER_QUANTITY_BELOW_USED("Total quantity cannot be lower than used quantity", HttpStatus.BAD_REQUEST);

    private final String message;
    private final HttpStatus httpStatus;

}
