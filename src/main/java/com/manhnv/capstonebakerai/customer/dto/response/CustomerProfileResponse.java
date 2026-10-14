package com.manhnv.capstonebakerai.customer.dto.response;

import com.manhnv.capstonebakerai.customer.enums.SkillLevel;
import com.manhnv.capstonebakerai.hobby.dto.HobbyResponse;

import java.util.List;
import java.util.UUID;

public record CustomerProfileResponse(
        UUID customerId,
        SkillLevel skillLevel,
        boolean emailVerified,
        List<HobbyResponse> hobbies
) {
}
