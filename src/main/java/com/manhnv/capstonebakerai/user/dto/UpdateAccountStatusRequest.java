package com.manhnv.capstonebakerai.user.dto;

import com.manhnv.capstonebakerai.user.enums.UserStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateAccountStatusRequest(
        @NotNull
        UserStatus status
) {
}
