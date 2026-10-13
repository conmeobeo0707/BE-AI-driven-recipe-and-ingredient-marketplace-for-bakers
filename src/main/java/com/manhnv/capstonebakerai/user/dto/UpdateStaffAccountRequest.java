package com.manhnv.capstonebakerai.user.dto;

import com.manhnv.capstonebakerai.user.enums.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateStaffAccountRequest(
        @NotBlank
        @Size(max = 100)
        String fullName,

        @NotNull
        Role role,

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
