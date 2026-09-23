package com.manhnv.capstonebakerai.identity.exception;

import com.manhnv.capstonebakerai.common.exception.AppException;

public class InvalidRegistrationRoleException extends AppException {
    public InvalidRegistrationRoleException() {
        super(IdentityErrorCode.INVALID_REGISTRATION_ROLE);
    }
}
