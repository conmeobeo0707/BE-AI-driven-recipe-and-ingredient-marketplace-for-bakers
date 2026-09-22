package com.manhnv.capstonebakerai.identity.exception;

public class VerificationEmailDeliveryException extends RuntimeException {

    public VerificationEmailDeliveryException(Throwable cause) {
        super("Verification email could not be sent", cause);
    }
}
