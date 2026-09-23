package com.manhnv.capstonebakerai.identity.service;

import com.manhnv.capstonebakerai.identity.dto.RegisterUserRequest;
import com.manhnv.capstonebakerai.identity.dto.RegisterUserResponse;

public interface RegistrationService {
    RegisterUserResponse register(RegisterUserRequest request);
}
