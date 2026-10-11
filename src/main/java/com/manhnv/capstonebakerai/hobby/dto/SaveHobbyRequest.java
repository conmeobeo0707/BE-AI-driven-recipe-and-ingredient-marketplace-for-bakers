package com.manhnv.capstonebakerai.hobby.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SaveHobbyRequest(
        @NotBlank
        @Size(max = 100)
        String name,

        @Size(max = 255)
        String description
) {
}