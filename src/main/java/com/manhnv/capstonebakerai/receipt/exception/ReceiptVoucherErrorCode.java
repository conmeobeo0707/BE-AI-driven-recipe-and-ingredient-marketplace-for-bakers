package com.manhnv.capstonebakerai.receipt.exception;

import com.manhnv.capstonebakerai.common.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ReceiptVoucherErrorCode implements ErrorCode {
    RECEIPT_VOUCHER_NOT_FOUND("Receipt voucher not found", HttpStatus.NOT_FOUND),
    RECEIPT_CATEGORY_NOT_MANUAL("Only OTHER category can be used for manual vouchers", HttpStatus.BAD_REQUEST),
    SYSTEM_RECEIPT_CATEGORY_MISSING("System receipt category is not configured", HttpStatus.INTERNAL_SERVER_ERROR),
    ;

    private final String message;
    private final HttpStatus httpStatus;
}
