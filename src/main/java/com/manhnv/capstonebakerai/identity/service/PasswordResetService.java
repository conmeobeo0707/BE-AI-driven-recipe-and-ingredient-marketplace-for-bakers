package com.manhnv.capstonebakerai.identity.service;

import com.manhnv.capstonebakerai.identity.dto.MessageResponse;

public interface PasswordResetService {
    MessageResponse requestCode(String email);

    MessageResponse verifyCode(String email, String code);

    MessageResponse resetPassword(String email, String code, String newPassword);
}
