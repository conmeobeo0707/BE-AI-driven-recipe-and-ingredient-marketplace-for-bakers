package com.manhnv.capstonebakerai.identity.exception;

public class InvalidRegistrationRoleException extends RuntimeException {

    public InvalidRegistrationRoleException() {
        super("Self-registration only supports LEARNER, STORE, or BAKER roles");
    }
}
