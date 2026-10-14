package com.manhnv.capstonebakerai.customer.dto.request;

import com.manhnv.capstonebakerai.customer.enums.SkillLevel;

public record UpdateCustomerProfileRequest(
        SkillLevel skillLevel
) {
}
