package com.manhnv.capstonebakerai.identity.exception;

public class InvalidVerificationCodeException extends RuntimeException {

    public InvalidVerificationCodeException() {
        super("Verification code is invalid or expired");
    }
}
