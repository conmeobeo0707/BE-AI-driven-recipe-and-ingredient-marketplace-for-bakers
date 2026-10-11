package com.manhnv.capstonebakerai.hobby.dto;

import java.util.UUID;

public record HobbyResponse(
        UUID id,
        String name,
        String description,
        boolean active
) {
}
