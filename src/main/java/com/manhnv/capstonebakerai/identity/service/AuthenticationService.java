package com.manhnv.capstonebakerai.identity.service;

import com.manhnv.capstonebakerai.identity.dto.AuthenticationResult;
import com.manhnv.capstonebakerai.identity.dto.LoginRequest;
import com.manhnv.capstonebakerai.identity.dto.MessageResponse;

public interface AuthenticationService {
    AuthenticationResult login(LoginRequest request);
    AuthenticationResult refresh(String rawRefreshToken);
    MessageResponse logout(String rawRefreshToken);
}
