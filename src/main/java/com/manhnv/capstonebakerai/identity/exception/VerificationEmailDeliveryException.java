package com.manhnv.capstonebakerai.identity.exception;

import com.manhnv.capstonebakerai.common.exception.AppException;

public class VerificationEmailDeliveryException extends AppException {
    public VerificationEmailDeliveryException(Throwable cause) {
        super(IdentityErrorCode.VERIFICATION_EMAIL_DELIVERY_FAILED);
        initCause(cause);
    }
}
