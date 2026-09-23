package com.manhnv.capstonebakerai.identity.exception;

import com.manhnv.capstonebakerai.common.exception.AppException;

public class EmailAlreadyExistsException extends AppException {
    public EmailAlreadyExistsException() {
        super(IdentityErrorCode.EMAIL_ALREADY_EXISTS);
    }
}
