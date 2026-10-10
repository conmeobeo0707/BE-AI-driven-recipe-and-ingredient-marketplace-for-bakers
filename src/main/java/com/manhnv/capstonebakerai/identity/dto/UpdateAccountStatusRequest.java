package com.manhnv.capstonebakerai.identity.dto;

import com.manhnv.capstonebakerai.user.enums.UserStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateAccountStatusRequest(
        @NotNull
        UserStatus status
) {
}
