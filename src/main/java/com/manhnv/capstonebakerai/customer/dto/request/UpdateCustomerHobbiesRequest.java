package com.manhnv.capstonebakerai.customer.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.Set;
import java.util.UUID;

public record UpdateCustomerHobbiesRequest(
        @NotNull
        Set<UUID> hobbyIds
) {
}
