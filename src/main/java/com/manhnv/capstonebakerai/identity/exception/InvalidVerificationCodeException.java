package com.manhnv.capstonebakerai.identity.exception;

import com.manhnv.capstonebakerai.common.exception.AppException;

public class InvalidVerificationCodeException extends AppException {
    public InvalidVerificationCodeException() {
        super(IdentityErrorCode.INVALID_VERIFICATION_CODE);
    }
}
