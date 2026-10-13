package com.manhnv.capstonebakerai.identity.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterUserRequest(
        @NotBlank(message = "Full name is required")
        @Size(max = 100, message = "Full name must not exceed 100 characters")
        String fullName,

        @NotBlank(message = "Email is required")
        @Email(message = "Email is invalid")
        @Size(max = 255, message = "Email must not exceed 255 characters")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 72, message = "Password must contain between 8 and 72 characters")
        String password,

        @Size(max = 20, message = "Phone must not exceed 20 characters")
        String phone,

        @Size(max = 255, message = "Address must not exceed 255 characters")
        String address,

        @Size(max = 10, message = "Date of birth must not exceed 10 characters")
        String dateOfBirth,

        @Size(max = 20, message = "Gender must not exceed 20 characters")
        String gender
) {
}
