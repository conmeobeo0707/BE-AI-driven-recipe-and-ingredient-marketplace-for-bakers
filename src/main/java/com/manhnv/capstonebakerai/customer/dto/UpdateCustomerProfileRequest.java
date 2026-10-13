package com.manhnv.capstonebakerai.customer.dto;

import com.manhnv.capstonebakerai.customer.enums.SkillLevel;

public record UpdateCustomerProfileRequest(
        SkillLevel skillLevel
) {
}
