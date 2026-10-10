package com.manhnv.capstonebakerai.user.dto;

import jakarta.validation.constraints.Size;

public record UpdateUserProfileRequest(
        @Size(max = 100)
        String fullName,

        @Size(max = 20)
        String phone,

        @Size(max = 255)
        String address,

        @Size(max = 10)
        String dateOfBirth,

        @Size(max = 20)
        String gender
) {
}
