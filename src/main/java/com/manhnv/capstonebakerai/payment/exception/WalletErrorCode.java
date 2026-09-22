package com.manhnv.capstonebakerai.payment.exception;

import com.manhnv.capstonebakerai.common.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum WalletErrorCode implements ErrorCode {
    WALLET_NOT_FOUND("Wallet not found", HttpStatus.NOT_FOUND),
    INVALID_TOPUP_AMOUNT("Invalid topup amount", HttpStatus.BAD_REQUEST);

    private final String message;
    private final HttpStatus httpStatus;



}
