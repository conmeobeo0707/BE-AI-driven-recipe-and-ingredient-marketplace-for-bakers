package com.manhnv.capstonebakerai.hobby.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateHobbyActiveRequest(
        @NotNull
        Boolean active
) {
}
